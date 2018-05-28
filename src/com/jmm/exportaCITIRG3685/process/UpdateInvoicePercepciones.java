/*
 * Proceso para generar los archivos exigidos por la RG3685: CITI compras y ventas
 * Diseño de los registros: http://www.afip.gob.ar/comprasyventas/
 * 
 * Autor: Juan Manuel Martínez - jmmartinezsf@gmail.com
 * Versión 0.1 - septiembre de 2015
 * Para Libertya 15.03
 */

package com.jmm.exportaCITIRG3685.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.openXpertya.model.MInvoice;
import org.openXpertya.model.MInvoiceLine;
import org.openXpertya.model.MInvoiceTax;
import org.openXpertya.model.Query;
import org.openXpertya.process.SvrProcess;
import org.openXpertya.util.CLogger;
import org.openXpertya.util.DB;

import com.jmm.exportaCITIRG3685.model.LP_C_Invoice;

public class UpdateInvoicePercepciones extends SvrProcess {
  
    
	protected void prepare() {

	}
	
	/**
	 * Crea/Actualiza la declaración del impuesto IVA 0%. 
	 * Para todas las facturas que tienen lineas con articulos
	 * excentos (IVA 0%), crea o actualiza el impuesto con el valor de la suma de las líneas.
	 **/
	private void fixExempts() throws Exception {	
		int taxID_iva0 = 1010087;

		String sql = "SELECT c_invoice_id, c_tax_id, SUM(linenetamt) " + 
				"FROM c_invoiceLine " + 
				"WHERE C_Tax_ID = ? " + 
				"GROUP BY c_invoice_id, c_tax_id";
		try {
		    PreparedStatement pstmt = DB.prepareStatement( sql );
		    pstmt.setInt( 1, taxID_iva0);
		    ResultSet rs = pstmt.executeQuery();
		    while(rs.next()) {
		        MInvoiceTax iTax = MInvoiceTax.get(getCtx(), rs.getInt(1), rs.getInt(2), get_TrxName());				
				if(iTax == null) {
					iTax = new MInvoiceTax(getCtx(), 0, get_TrxName());
					iTax.setC_Invoice_ID(rs.getInt(1));
					iTax.setC_Tax_ID(rs.getInt(2));					
				} 
				/*
				 * En este caso se debe poner el importe
				 * del impuesto en 0 y la base con el neto de la suma de las lineas. De esta manera lo extrae bien
				 * el componente CITI y tambien evita la doble suma al total de la factura.
				 */
				iTax.setTaxBaseAmt(rs.getBigDecimal(3));
				iTax.setTaxAmt(BigDecimal.ZERO);
				if (!iTax.save()) {
					throw new Exception(CLogger.retrieveErrorAsString());
				} 
		    }
		    rs.close();
		    pstmt.close();
		} catch( SQLException e ) {
		    log.log( Level.SEVERE,"Fixing tax 0%",e );
		}
	}
	
	/**
	 * Goizueta cargaba mal las facturas de compras, cuando necesitaba agregar una percepción de IIBB 
	 * o de IVA, en vez de crearlo en la pestaña "Otros impuestos" lo agregaba como una linea mas de la factura.
	 **/
	private void fixPerceptions() throws Exception {		
		int taxID_percepciones_iibb = 1010163;
		int taxID_percepciones_iva = 1010164;		
		
		List<Object> params = new ArrayList<Object>();
		StringBuffer whereClause = new StringBuffer();
		whereClause.append("C_Tax_ID = ? or C_Tax_ID = ?");// or C_Tax_ID = ?");
		params.add(taxID_percepciones_iibb);
		params.add(taxID_percepciones_iva);
		Query q = new Query(this.getCtx(), MInvoiceLine.Table_Name, whereClause.toString(), get_TrxName());
		q.setParameters(params);
		List<MInvoiceLine> result = q.list();
		
		for(MInvoiceLine il: result) {
			MInvoiceTax iTax = MInvoiceTax.get(getCtx(), il.getC_Invoice_ID(), il.getC_Tax_ID(), get_TrxName());
			
			if(iTax == null) {
				iTax = new MInvoiceTax(getCtx(), 0, get_TrxName());
				iTax.setC_Tax_ID(il.getC_Tax_ID());
				iTax.setC_Invoice_ID(il.getC_Invoice_ID());
			}	
		
			/*
			 * Las percepciones se quitan de las lineas de la factura y se agregan
			 * como "otros Impuestos".
			 * Para que aparezca como "Otro Impuesto" y no como "Impuesto de Factura", la
			 * categoría del impuesto debe aparecer como "Manual".
			 * La base del impuesto debe ir en 0 y el importe con el valor deseado.
			 */
			iTax.setTaxAmt(new BigDecimal(il.getLineNetAmount().toString()));
			iTax.setTaxBaseAmt(BigDecimal.ZERO);
			MInvoice invoice = new MInvoice(getCtx(), il.getC_Invoice_ID(), get_TrxName());
			il.delete(true);
			invoice.save();
			if (!iTax.save()) {
				throw new Exception(CLogger.retrieveErrorAsString());
			} 
		}
	}
	
	/**
	 * Repara todas las facturas que tienen el campo afipDocType en NULL
	 * Solo basta con hacer que ejecuten el save, para que se ejecute el preBeforeSave.
	 **/
	private void fixAfipDocType() {
		StringBuffer whereClause = new StringBuffer();
		whereClause.append("afipDocType IS NULL");
		Query q = new Query(this.getCtx(), MInvoice.Table_Name, whereClause.toString(), get_TrxName());
		List<MInvoice> result = q.list();
		System.out.println("# Facturas sin afipDocType: "+result.size());
		for(MInvoice i: result) {
			LP_C_Invoice invoice = new LP_C_Invoice(i.getCtx(), i.getID(), i.get_TrxName());
			if(invoice.getafipdoctype() == null || invoice.getafipdoctype().isEmpty()){
				String guessedAFIPDocType = com.jmm.exportaCITIRG3685.model.MInvoice.guessAFIPDocType(invoice.getCtx(), invoice.isSOTrx(), invoice.getC_Letra_Comprobante_ID(), invoice.getC_DocType_ID());				
				if(guessedAFIPDocType!=null)
					invoice.setafipdoctype(guessedAFIPDocType);
			}
			invoice.save();
		}
	}
	
	protected String doIt() throws java.lang.Exception {
		try {
			fixAfipDocType();
			fixExempts();
			fixPerceptions();			
		} catch (Exception e) {
			e.printStackTrace();
		}
 		return "";
	}				

}
