/*
 * Proceso para generar los archivos exigidos por la RG3685: CITI compras y ventas
 * Diseño de los registros: http://www.afip.gob.ar/comprasyventas/
 * 
 * Autor: Juan Manuel Martínez - jmmartinezsf@gmail.com
 * Versión 0.1 - septiembre de 2015
 * Para Libertya 15.03
 */

package com.jmm.exportaCITIRG3685.process;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import org.openXpertya.model.MInvoiceLine;
import org.openXpertya.model.MInvoiceTax;
import org.openXpertya.model.MPeriod;
import org.openXpertya.model.Query;
import org.openXpertya.process.ProcessInfoParameter;
import org.openXpertya.process.SvrProcess;
import org.openXpertya.util.DB;
import org.openXpertya.util.Env;

import com.jmm.exportaCITIRG3685.model.LP_C_Tax;
import com.jmm.exportaCITIRG3685.model.MInvoice;

import general.utils.Swissknife;

public class ExportaCITI extends SvrProcess {
  
    private MPeriod paramPeriodo;
    private String paramTipoTransaccion;
    private String paramCarpetaSalida = "";
    private ResultSet rs = null;
	private int invoiceId;
	boolean isSOTrx;
	private Double montoIVA, montoPercepcionesIVA, montoPercepcionesIIBB
			, montoPercepcionesImpNacionales, montoPercepcionesImpMunicipales, montoImpOtros
			, montoOperacionesExentas; 

	private static final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");	
	private static final DecimalFormat decimalFormat = new DecimalFormat("#.00");

    private static final String lineSeparator = "\r\n";	
	
	private static final String QUERY = 			
			"select \n" + 
			"	inv.c_invoice_id\n" + 
			"	, inv.dateacct::date\n" + 
			"	, inv.dateinvoiced::date\n" + 
			"	, inv.afipdoctype\n" + 
			"	, inv.documentno\n" + 
			"	, inv.grandtotal\n" +
			"	, case \n" + 
			"		when bp.taxidtype is not null then bp.taxidtype \n" + 
			"		when coalesce(inv.nroidentificcliente,'') = '' then '99' \n" +
			"		else '96' \n" +
			"	end as taxidtype \n" +
			"	, case \n" +
			"		when bp.taxid = '' or bp.taxid is null then coalesce(inv.nroidentificcliente,'') \n" +
			"		else bp.taxid \n" +
			"	end as taxid \n" + 
			"	, bp.name\n" + 
			"	, sum(case when itax.taxbaseamt = 0 and itax.taxamt != 0 then inv.netamount else itax.taxbaseamt end)\n" + 
			"	, sum(itax.taxamt)\n" + 
			"	, cur.wsfecode\n" + 
			"	, tax.citirg3685\n" +
			"	, tax.WSFEcode \n" +			
			"	, ltr.letra \n" +
			"from c_invoicetax itax \n" + 
			"join c_invoice inv on itax.c_invoice_id = inv.c_invoice_id \n" + 
			"join c_doctype dt on inv.c_doctype_id = dt.c_doctype_id \n" + 
			"join c_bpartner bp on inv.c_bpartner_id = bp.c_bpartner_id \n" + 
			"join c_currency cur on inv.c_currency_id = cur.c_currency_id \n" + 
			"join c_tax tax on itax.c_tax_id = tax.c_tax_id \n" + 
			"join c_letra_comprobante ltr on inv.c_letra_comprobante_id = ltr.c_letra_comprobante_id \n" + 
			"where \n" + 
			"	inv.dateacct::date between ? and ? \n" + 
			"	and (dt.docsubtypeinv in ('SF') or dt.isfiscaldocument = 'Y') \n" + 
			"	and inv.docstatus in ('CO', 'CL', 'RE') \n" + 
			"	and inv.issotrx = ? \n" + 
			"   and (itax.taxbaseamt != 0 or itax.taxamt != 0) \n" +
			"   and inv.isactive = 'Y' \n" +
			"group by 1, 2, 3, 4, 5, 6, 7, 8, 9, 12 , 13, 14, 15 \n" +
			"order by \n" + 
			"	3 asc, inv.c_invoice_id, bp.name, inv.documentno, sum(itax.taxamt) desc\n"
			;

	private static final int IX_INVOICE_ID 				= 1;
	private static final int IX_INVOICE_DATE_ACCT 		= 2;
	private static final int IX_INVOICE_DATE_INVOICED 	= 3;
	private static final int IX_INVOICE_AFIP_DOCTYPE 	= 4;
	private static final int IX_INVOICE_DOCUMENT_NO 	= 5;
	private static final int IX_INVOICE_GRANDTOTAL 		= 6;
	private static final int IX_BP_TAX_ID_TYPE 			= 7;
	private static final int IX_BP_TAX_ID 				= 8;
	private static final int IX_BP_NAME 				= 9;
	private static final int IX_INVOICE_TAX_AMT_BASE 	= 10;
	private static final int IX_INVOICE_TAX_AMT 		= 11;
	private static final int IX_CURRENCY_WSFECODE 		= 12;
	private static final int IX_TAX_CITI_REF 			= 13;
	private static final int IX_TAX_WSFE_CODE 			= 14;
	private static final int IX_LETRA 					= 15;
	
	protected void prepare() {
		borraImpuestos();
		
		ProcessInfoParameter[] para = getParameter();
		for(int i = 0;i < para.length;i++) {
			log.fine("prepare - " + para[i]);

            String name = para[i].getParameterName();

            if(para[i].getParameter() == null)
                ;
            else if (name.equalsIgnoreCase("Periodo")){
            	// don't ask
            	int id;
            	Object parameter = para[i].getParameter();
            	if(parameter instanceof String)
            		id = Integer.valueOf((String)para[i].getParameter());
            	else
            		id = ((BigDecimal) para[i].getParameter()).intValueExact();
            	
            	paramPeriodo = MPeriod.get(Env.getCtx(), id, Env.getCtx().getProperty(name));
            } else if(name.equalsIgnoreCase("TipoTrans")) 
            	paramTipoTransaccion = (String)para[i].getParameter();
             else if(name.equalsIgnoreCase("Directorio")) 
            	paramCarpetaSalida = (String)para[i].getParameter();
             else
                log.log(Level.SEVERE,"prepare - Parámetro desconocido: " + name);
        }

	}
	
	protected String doIt() throws java.lang.Exception {
		isSOTrx = !paramTipoTransaccion.equalsIgnoreCase("V") ;
		
		File targetDir = new File (paramCarpetaSalida);
		if (!targetDir.exists())
			targetDir.mkdir();
		
		String archivo_cbte = paramCarpetaSalida + File.separator + "REGINFO_CV_" + 
							  (isSOTrx ? "VENTAS" : "COMPRAS") + "_CBTE_" +
							  paramPeriodo.getName().toUpperCase() + ".txt";
		String archivo_alic = paramCarpetaSalida + File.separator + "REGINFO_CV_" + 
							  (isSOTrx ? "VENTAS" : "COMPRAS" )  + "_ALICUOTAS_" +
							  paramPeriodo.getName().toUpperCase() + ".txt";
		String result ="";
		String lineSeparator = System.getProperty("line.separator");
        FileWriter cbtes = null, alic = null;
        PreparedStatement pstmt = null;
 		
        try
 		{
 			long inicia = System.currentTimeMillis();
 			
 			/*
 			 * Necesito poder navegar hacia adelante y atrás en el resulset, por eso creo de esta manera
 			 * el preparestatement.
 			 */
 			pstmt = DB.getConnectionRW().prepareStatement(QUERY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
 			pstmt.setTimestamp(1, paramPeriodo.getStartDate());
 			pstmt.setTimestamp(2, paramPeriodo.getEndDate());
 			pstmt.setString(3, isSOTrx ? "Y" : "N");
 			rs = pstmt.executeQuery();
 			
 			Object[] prs = {paramPeriodo.getStartDate(), paramPeriodo.getEndDate(), isSOTrx ? "Y" : "N"};
 			Swissknife.sqlToPrint(QUERY, prs, "Consulta CITI");
 			
 			cbtes = new FileWriter(archivo_cbte);
 			alic = new FileWriter(archivo_alic);
 			int Q = creaArchivos(cbtes, alic);
 			cbtes.close();
 			alic.close();
		
 			long termina = System.currentTimeMillis();
 			long segundos = (termina-inicia)/1000;
 			log.log(Level.SEVERE,"ExportaCITI RG3685 Tardo: "+segundos+" segundos - " + Q + " registros.");
 			rs.close();
 			pstmt.close();
 			pstmt = null;
 		}
 		catch (Exception e)
 		{ 
 			log.saveError("Exportacion CITI RG3685 - Prepare", e);
 			e.printStackTrace();
 			
 			try {
				if(cbtes != null) cbtes.close();
				if(alic != null) alic.close();
				if(pstmt != null) pstmt.close();
				if(rs != null)rs.close();					
			} catch (IOException e1) {
				result = "Error al generar los archivos CITI RG3685 " +lineSeparator + e.getLocalizedMessage();
				e1.printStackTrace();
			}
 			
 			throw e; 			
 		}	
 		return result;
	}				
	
	private int creaArchivos(FileWriter fw_c , FileWriter fw_a) throws Exception {
		int cant = 0;
		int q_alic = 0;
		
		StringBuffer la; 				// linea de alícuotas
		StringBuffer lc; 				// línea de comprobantes
		
		String cmpFecha;
		String cmpTipo ;
		String cmpPuntoVenta;
		String cmpNumero;
		String cmpLetra;
		String cmpTotal ;
		
		String noGravado; //Agregado por Geneos
		
		String bpCodigoIdentificadorFiscal;
		String bpIdentificadorFiscal;
		String bpNombre;
		
		String monedaCodigoWSFE;
		String citiReference = "";
		Integer operacionCondicionIVA;

		
		while(rs.next())
		{
			/* 
			 * Primero genero las líneas de las alícuotas y voy calculando las sumas totalizadoras del
			 * informe por comprobante, que va en un archivo separado.
			*/

            bpNombre = padLeft(formatString(rs.getString(IX_BP_NAME).toUpperCase()), 30);
            
            if(rs.getString(IX_BP_TAX_ID)==null)
                throw new Exception(bpNombre + " no tiene un identificador fiscal cargado");          

			cmpFecha = formatDate(rs.getDate(IX_INVOICE_DATE_INVOICED));
			cmpTipo = padLeft(rs.getString(IX_INVOICE_AFIP_DOCTYPE), 3, '0');
			cmpPuntoVenta = padLeft(rs.getString(IX_INVOICE_DOCUMENT_NO).substring(1, 5), 5, '0');
			cmpNumero = padLeft(rs.getString(IX_INVOICE_DOCUMENT_NO).substring(6, 13), 20, '0');
			bpCodigoIdentificadorFiscal = rs.getString(IX_BP_TAX_ID_TYPE);
			bpIdentificadorFiscal = padLeft(rs.getString(IX_BP_TAX_ID).replace("-", ""), 20, '0');
			cmpTotal = formatNumber(rs.getDouble(IX_INVOICE_GRANDTOTAL), 15);
			monedaCodigoWSFE = rs.getString(IX_CURRENCY_WSFECODE);
			cmpLetra = rs.getString(IX_LETRA).toUpperCase();
			
			citiReference = rs.getString(IX_TAX_CITI_REF);
			operacionCondicionIVA = rs.getObject(IX_TAX_WSFE_CODE) == null ? null : rs.getInt(IX_TAX_WSFE_CODE);
			
			if(citiReference==null)
				throw new Exception("No se pudo determinar el código de impuesto para " + rs.getString(IX_INVOICE_DOCUMENT_NO));
			citiReference = citiReference.trim().toUpperCase();

			if (cmpLetra.equals("A") || (cmpLetra.equals("B") && isSOTrx) || cmpLetra.equals("M") || esOtros(cmpTipo)){
				la = new StringBuffer();
				la.append(cmpTipo);
				
 				if (esOtros(cmpTipo))
 	 				la.append("00000");
 				else
 	 				la.append(cmpPuntoVenta);
 				
 				la.append(cmpNumero);
 				if (!isSOTrx){			// los campos 6 y 7 no van para informes de ventas
 					la.append(bpCodigoIdentificadorFiscal);
 					la.append(bpIdentificadorFiscal);
 				}

 				/*
 				 * Fix realizado por Geneos para evitar que los impuestos excentos
 				 * se exporten en las alicuotas
 				 */
 				boolean write = false;
 				Double baseTax = 0.00;
 				Double importTax = 0.00;
 				if (esCreditoDebitoFiscal(citiReference)) {
 					baseTax = rs.getDouble(IX_INVOICE_TAX_AMT_BASE);
 					importTax = rs.getDouble(IX_INVOICE_TAX_AMT);
 					write = true;
 				} else if (q_alic == 0 && citiReference.equals(LP_C_Tax.CITIRG3685_ImportesExentos))
 					write = true;
 				if(write){
					if(operacionCondicionIVA==null)
						throw new OperacionCondicionIVAFaltanteException(cmpNumero);				
	 				la.append(formatNumber(baseTax, 15));
	 				la.append(padLeft(operacionCondicionIVA.toString(), 4, '0')); // Alícuota de IVA
	 				la.append(formatNumber(importTax, 15)); // IVA liquidado
					
	 				q_alic++;
	 				la.append(lineSeparator);
	 				fw_a.write(la.toString());
 				}
 				/*
 				 * Fin modificacion de Geneos
 				 */
 				
 				
 				/*boolean write = false;
 				
 				if (esCreditoDebitoFiscal(citiReference))
 					write = true;
 				// resultset ordered by tax amount
 				else if (q_alic == 0 && citiReference.equals(LP_C_Tax.CITIRG3685_ImportesExentos))
 					write = true;
 				 				
 				if(write){
 					if(operacionCondicionIVA==null)
 						throw new OperacionCondicionIVAFaltanteException(cmpNumero);
 					
 	 				la.append(formatNumber(rs.getDouble(IX_INVOICE_TAX_AMT_BASE), 15));
 	 				la.append(padLeft(operacionCondicionIVA.toString(), 4, '0')); // Alícuota de IVA
 	 				la.append(formatNumber(rs.getDouble(IX_INVOICE_TAX_AMT), 15)); // IVA liquidado
 					
	 				q_alic++;
	 				la.append(lineSeparator);
	 				fw_a.write(la.toString());
 				}*/
			}
			
			/*
			 * AcumulaImportes agrega los montos de impuestos a cada variable. Si devuelve verdadero, es porque
			 * la próxima línea es de otro comprobante por lo que debo generar la línea del comprobante. Lo mismo
			 * ocurre si es la última línea del rs.
			 */
			if (acumulaImportes(rs.getInt(IX_INVOICE_ID)) || rs.isLast()){
				lc = new StringBuffer();
				lc.append(cmpFecha);
				lc.append(cmpTipo);
				if (esOtros(cmpTipo))
					lc.append("00000");
				else
					lc.append(cmpPuntoVenta);
				lc.append(cmpNumero);
				if (!isSOTrx)
					// TODO: las importaciones no están soportadas por esta versión.
					lc.append("                ");
				else
					lc.append(cmpNumero);
				
				/*
				 * Modificación de Geneos.
				 * Para las ventas a consumidores finales, superiores a $1000, se debe cambiar 
				 * el "Código de documento del comprador" de 99 a 96
				 */
				if(cmpLetra.equals("B") && isSOTrx && Integer.parseInt(cmpTotal) > 100000) {
					lc.append("96");
				} else {
					lc.append(bpCodigoIdentificadorFiscal);
				}
				//lc.append(bpCodigoIdentificadorFiscal);
				/*
				 * Fin modificacion Geneos
				 */
				
				lc.append(bpIdentificadorFiscal);
				lc.append(bpNombre);
				lc.append(cmpTotal);
				
				/*
				 * 	Modificación para considerar los no gravados
				 * 	Geneos 03/12/2016
				 *  
				 */
				Double nograv = 0.00;
				if(cmpLetra.equals("B") || cmpLetra.equals("C")) {
					lc.append(formatNumber(0d, 15));
				} else {				
					nograv = calcularNoGravados(rs.getInt(IX_INVOICE_ID));
					noGravado = formatNumber(nograv, 15);
					lc.append(noGravado);
				}
				
				// Parche impresentable para hacer que dé el total
				// No se la agarren con el desarrollador!
				if(nograv > 0.00) {
					montoOperacionesExentas = 0.00;
				}
				
				// Extrae las percepciones de IVA desde las lineas de las facturas
				// Este parche no es necesario si se saca la percepcion de la linea de la fact
				// y se la coloca como "otro impuesto"
				//montoPercepcionesIVA = calcularPercepcionesIVA(rs.getInt(IX_INVOICE_ID));
				
				/*
				 * Fin modificación de Geneos
				 */
				
				if(cmpLetra.equals("B") || cmpLetra.equals("C") || montoIVA == 0){
					//lc.append(formatNumber(0d, 15));
					lc.append(formatNumber(0d, 15));
				}else {
					//lc.append(formatNumber(montoOperacionesExentas, 15));
					lc.append(formatNumber(0d, 15));					
				}
				lc.append(formatNumber(montoPercepcionesIVA, 15));
				lc.append(formatNumber(montoPercepcionesImpNacionales, 15));
				lc.append(formatNumber(montoPercepcionesIIBB, 15));
				lc.append(formatNumber(montoPercepcionesImpMunicipales, 15));
				// TODO: dar soporte a impuestos internos
				lc.append("000000000000000");
				// TODO: dar soporte multimoneda
				lc.append(monedaCodigoWSFE);
				lc.append("0001000000");  // Tipo de cambio fijo, ya que no hay multimoneda
				if(!isSOTrx && (cmpLetra.equals("B") || cmpLetra.equals("C")))
					lc.append(0);
				else
					lc.append(q_alic == 0 ? 1 : q_alic);
				// Tipo de operación
				if ((cmpLetra.equals("A") || cmpLetra.equals("B") || cmpLetra.equals("C") || cmpLetra.equals("M") || esOtros(cmpTipo)) 
				        && montoIVA == 0.0)
					lc.append("E");
				else
					lc.append(" ");
				if (!isSOTrx)
					lc.append(formatNumber(montoIVA, 15));
				lc.append(formatNumber(montoImpOtros, 15));
				if (!isSOTrx)
					// TODO: dar soporte para comisiones de corredores
					lc.append("00000000000                              000000000000000");
				if (isSOTrx)
					lc.append(cmpFecha);
				lc.append(lineSeparator);
 				fw_c.write(lc.toString());
 				//fw_c.write("\r");
 				lc = null;
 				q_alic = 0;
			}
			
			/*
			 * Guardo el  invoice_id del comprobante por que si en la próxima línea del rs 
			 * el invoice_id tiene el mismo valor entonces tengo que acumular los montos de impuestos.
			 */
			invoiceId = rs.getInt(IX_INVOICE_ID);
			cant++;
		}
	
		return cant;
			
	}
	
	/**
	 * Retorna la fecha en formato compatible con el aplicativo
	 */
    private String formatDate(Date fecha) {
        return dateFormat.format(fecha);
    }

    /**
     * Rellena (hacia la izquierda) una cadena con caracteres en blanco
     */
    private String padLeft(String str, int length){
        return padLeft(str, length, ' ');
    }
    
    /**
     * Rellena cadena hacia la izquierda     
     * @param str Cadena a ser rellenada (de ser necesario)
     * @param length Largo total de la cadena resultante. Si la cadena original
     *          es mas larga entonces se truncan los caracteres sobrantes.
     * @param padding Caracter de relleno
     * @return
     */
    private String padLeft(String str, int length, char padding){
        if(str==null)
            str = "";
        
        if(str.length() > length)
            return str.substring(0, length);
        
        StringBuilder sb = new StringBuilder();

        for (int i=length-str.length(); i>0; i--) {
            sb.append(padding);
        }        

        sb.append(str);
        return sb.toString();        
    }

    /**
     * Formateando números al estilo AFIP
     * Al que diseñó este formato le deseamos que haya encontrado finalmente su vocación ;)
     * @param number
     * @param length
     * @return
     */
    private String formatNumber(Double number, int length){
        String result = decimalFormat.format(Math.abs(number))
                        .replace(".", "").replace(",", "");
        if(number<0)
            return "-" + padLeft(result, length - 1, '0');
        
        return padLeft(result, length, '0');
    }
    
    /**
     * Conversión de cadenas a un formato compatible con el aplicativo
     * @param str
     * @return
     */
    private String formatString(String str){
        // Normalizo y reemplazo caracteres no ASCII por #
        return Normalizer.normalize(str, Normalizer.Form.NFD).replaceAll("[^\\x00-\\x7F]", "#");
    }    
    
    /**
     * Borra los montos acumulados 
     */
    private void borraImpuestos(){
    	montoIVA = 0.0;
    	montoPercepcionesIVA = 0.0;
    	montoPercepcionesIIBB = 0.0;
    	montoPercepcionesImpNacionales = 0.0;
    	montoPercepcionesImpMunicipales = 0.0;
    	montoImpOtros = 0.0;
    	montoOperacionesExentas = 0.00;
    }
    
    /**
     * Devuelve verdadero si el id del impuesto consultado corresponde a uno configurado 
     * como crédito o débto fiscal
     */
    private Boolean esCreditoDebitoFiscal(String reference){
    	return reference.equals(LP_C_Tax.CITIRG3685_CréditoODébitoFiscalIVA);
    }
    
    /**
     * Devuelve verdadero si el tipo de comprobante es "Otros comprobantes" u "Otros comprobantes - credito".
     */
    private Boolean esOtros(String tipo){
    	return tipo.equals(MInvoice.AFIPDOCTYPE_OtrosComprobantes_NotasDeCrédito) 
    			|| tipo.equals(MInvoice.AFIPDOCTYPE_OtrosComprobantes);
    }
    
    /**
     * Acumula los importes de las líneas de cada factura.
     * @return true si la próxima línea del rs corresponde a la misma factura que la actual línea
     *          , false en caso contrario
     */
    private Boolean acumulaImportes(int id) throws SQLException{
    	
    	Boolean ret = false;

    	/*
		 * Chequeo si el invoice_id de ésta línea es el mismo que el que está guardado y reseteo los montos
		 * de impuestos de ser necesario
		 */
		if (invoiceId != rs.getInt(IX_INVOICE_ID))
			borraImpuestos();

		if (rs.isLast()) // Si estoy en la última línea, devuelvo true para que se guarde el comprobante.
    		ret = true;
		else{
	    	rs.next();
	    	ret = !(rs.getInt(IX_INVOICE_ID) == id);
	    	rs.previous();
		}
		
		Double taxAmount = rs.getDouble(IX_INVOICE_TAX_AMT);
		Double taxBaseAmount = rs.getDouble(IX_INVOICE_TAX_AMT_BASE);
		
		String reference = rs.getString(IX_TAX_CITI_REF).trim().toUpperCase();
		if (reference.equals(LP_C_Tax.CITIRG3685_CréditoODébitoFiscalIVA))
			montoIVA += taxAmount;
		
		else if (reference.equals(LP_C_Tax.CITIRG3685_PercepcionesDeIVA))
			montoPercepcionesIVA += taxAmount;
		
		else if (reference.equals(LP_C_Tax.CITIRG3685_PercepcionesDeIngresosBrutos))
			montoPercepcionesIIBB += taxAmount;
		
		else if (reference.equals(LP_C_Tax.CITIRG3685_PercepcionesDeImpuestosNacionales))
			montoPercepcionesImpNacionales += taxAmount;
		
		else if (reference.equals(LP_C_Tax.CITIRG3685_PercepcionesDeImpuestosMunicipales))
			montoPercepcionesImpMunicipales += taxAmount;
		
		else if (reference.equals(LP_C_Tax.CITIRG3685_OtrosImpuestos))
			montoImpOtros += taxAmount;
		
		else if (reference.equals(LP_C_Tax.CITIRG3685_ImportesExentos))
			montoOperacionesExentas += taxBaseAmount;
		
		return ret;
    }
        
    @SuppressWarnings("serial")
	public class OperacionCondicionIVAFaltanteException extends Exception {
    	public OperacionCondicionIVAFaltanteException(String documentNo){
    		super("No está configurada la Operación / condición de IVA / Alícuota de IVA / WSFE para " + documentNo);
    	}
    }
    
    
    /**
	 * Método creado por Geneos para poder extraer de las líneas de facturas
	 * los conceptos no grabados.
     */
    private Double calcularNoGravados(int invoice_id) {
		/*
		 * Dado que el total de los importes No gravados se colocó en la base del impuesto IVA 0%
		 * se lo toma desde ahí.
		 * Este fix se hace en UpdateInvoicePercepciones.fixExempts
		 */
    	int taxID_iva0 = 1010087;
    	List<Object> params = new ArrayList<Object>();
    	final StringBuffer whereClause = new StringBuffer();
    	whereClause.append("C_Invoice_ID = ? and C_Tax_ID = ?");
    	params.add(invoice_id);
    	params.add(taxID_iva0);
    	Query q = new Query(this.getCtx(), MInvoiceTax.Table_Name, whereClause.toString(), this.get_TrxName());
    	q.setParameters(params);
    	MInvoiceTax result = q.first();
    	return result != null ? result.getTaxBaseAmt().doubleValue() : 0D;
	}
    
    private Double calcularPercepcionesIVA(int invoiceID) {
    	int taxID_percepciones_iva = 1010164;   	
    	List<Object> params = new ArrayList<Object>();
		StringBuffer whereClause = new StringBuffer();
		whereClause.append("C_Invoice_ID = ? and C_Tax_ID = ?");
		params.add(invoiceID);
		params.add(taxID_percepciones_iva);
		Query q = new Query(this.getCtx(), MInvoiceLine.Table_Name, whereClause.toString(), get_TrxName());
		q.setParameters(params);
		MInvoiceLine result = q.first();
		return result != null ? result.getLineNetAmount().doubleValue() : 0D;
    }

}
