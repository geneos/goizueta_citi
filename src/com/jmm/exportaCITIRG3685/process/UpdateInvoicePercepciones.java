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

public class UpdateInvoicePercepciones extends SvrProcess {
  
    
	protected void prepare() {

	}
	
	/**
	 * Goizueta cargaba mal las facturas de compras, cuando necesitaba agregar una percepción de IIBB 
	 * o de IVA, en vez de crearlo en la pestaña "Otros impuestos" lo agregaba como una linea mas de la factura.
	 **/
	private void fixTaxsInvoices() throws Exception {		
		int taxID_percepciones_iibb = 1010163;
		int taxID_percepciones_iva = 1010164;		
		int taxID_iva0 = 1010087;
		
		List<Object> params = new ArrayList<Object>();
		StringBuffer whereClause = new StringBuffer();
		whereClause.append("C_Tax_ID = ? or C_Tax_ID = ? or C_Tax_ID = ?");
		params.add(taxID_percepciones_iibb);
		params.add(taxID_percepciones_iva);
		params.add(taxID_iva0);
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
			 * Si el impuesto de la línea es una Percepción de IIBB, se debe:
			 * Colocar la base del impuesto en 0 y el importe con el neto de la linea, para que lo levante
			 * el componente CITI.
			 * Eliminar la linea de la factura, que ya no es necesaria. Ademas evita que lo sume 2 veces
			 * Actualizar la factura para que actualice el monto total
			 */
			if(iTax.getC_Tax_ID() == taxID_percepciones_iibb) {
				iTax.setTaxAmt(new BigDecimal(il.getLineNetAmount().toString()));
				iTax.setTaxBaseAmt(BigDecimal.ZERO);
				MInvoice invoice = new MInvoice(getCtx(), il.getC_Invoice_ID(), get_TrxName());
				il.delete(true);
				invoice.save();
			}
			
			/*
			 * Las percepciones de IVA se quitan de las lineas de la factura y se agregan
			 * como "otros Impuestos", al igual que las percepciones de IIBB.
			 * Para que aparezca como "Otro Impuesto" y no como "Impuesto de Factura", la
			 * categoría del impuesto debe aparecer como "Manual".
			 * Por el momento es igual al codigo java de percepciones de IIBB, pero se 
			 * lo deja igual.
			 */
			if(iTax.getC_Tax_ID() == taxID_percepciones_iva) {
				iTax.setTaxAmt(new BigDecimal(il.getLineNetAmount().toString()));
				iTax.setTaxBaseAmt(BigDecimal.ZERO);
				MInvoice invoice = new MInvoice(getCtx(), il.getC_Invoice_ID(), get_TrxName());
				il.delete(true);
				invoice.save();
			}
			
			/*
			 * Si es un articulo no gravado, tiene IVA 0%. En este caso se debe poner el importe
			 * del impuesto en 0 y la base con el neto de la linea. De esta manera lo extrae bien
			 * el componente CITI y tambien hace al total de la factura.
			 */
			if(iTax.getC_Tax_ID() == taxID_iva0) {
				iTax.setTaxAmt(BigDecimal.ZERO);
				iTax.setTaxBaseAmt(il.getLineNetAmount());
			}
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
		for(MInvoice i: result) {
			i.save();
		}
	}
	
	private void actualizaFacturas() throws Exception {
		// TODO Auto-generated method stub
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		int taxID_percepciones_iibb = 1010163;
		int taxID_percepciones_iva = 1010164;
		
		int taxID_iva0 = 1010087;
		
		String consulta = "select c_tax_id, c_invoice_id, linenetamt, taxamt from c_invoiceline where C_Tax_ID=" + taxID_percepciones_iibb + " or C_Tax_ID=" + taxID_percepciones_iva;
				
		try {
			pstmt = DB.prepareStatement(consulta, get_TrxName());
			rs = pstmt.executeQuery();
			MInvoiceTax newInvoiceTax = null;
			MInvoiceTax invoiceTaxIva0 = null;
			
			int flagCambio = 1;
			int lastInvoice = 0;
			MInvoiceTax iTax = null;

			while (rs.next()) {
				
				if(rs.getInt(2) == lastInvoice)
					flagCambio = 0;
				else {
					flagCambio = 1;
					lastInvoice = rs.getInt(2);
				}
				
				iTax = MInvoiceTax.get(getCtx(), lastInvoice, rs.getInt(1), get_TrxName());
				
				if (iTax != null) {
					// Actualizo el registro de percepciones
//					iTax.setTaxBaseAmt(iTax.getTaxBaseAmt().add(rs.getBigDecimal(3)));
					iTax.setTaxBaseAmt(rs.getBigDecimal(3));
					if (!iTax.save()) {
						throw new Exception(CLogger.retrieveErrorAsString());
					}					
				} else {
					// Creo el registro de percepciones
					newInvoiceTax = new MInvoiceTax(getCtx(), 0, get_TrxName());
					newInvoiceTax.setC_Tax_ID(rs.getInt(1));
					newInvoiceTax.setC_Invoice_ID(rs.getInt(2));
					newInvoiceTax.setTaxAmt(rs.getBigDecimal(4));
					newInvoiceTax.setTaxBaseAmt(rs.getBigDecimal(3));
					if (!newInvoiceTax.save()) {
						throw new Exception(CLogger.retrieveErrorAsString());
					}					
				}
				
				
				// Actualizo el registro de IVA 0%
				
				invoiceTaxIva0 = MInvoiceTax.get(getCtx(), rs.getInt(2), 1010087, get_TrxName());				
				invoiceTaxIva0.setTaxBaseAmt(rs.getBigDecimal(3));
				if (!invoiceTaxIva0.save()) {
					throw new Exception(CLogger.retrieveErrorAsString());
				}
				
			}
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Fill T_CuentaCorriente error", e);
			throw new Exception("Current Account Error", e);
		}
		
		
	}

	protected String doIt() throws java.lang.Exception {

		try {
			fixAfipDocType();
			fixTaxsInvoices();			
		} catch (Exception e) {
			e.printStackTrace();
		}
 		return "";
	}				

}
