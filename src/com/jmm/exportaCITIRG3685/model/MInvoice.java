/*
 * Extensión de la clase MInvoice para el plugin de exportación de archivos CITI RG3685
 * 
 * Los tipos de documentos definidos por AFIP deben informarse por cada documento. En los informes de ventas
 * no hay inconvenientes, ya que cada tipo de documento (factura, ND, NC) tiene su valor proveniente de DocType.
 * En el caso de las compras, el problema es que hay dos tipos básicos de documento (Factura o abono de proveedor)
 * por lo que es necesario definir el tipo al momento de la carga del comprobante.
 * Si no se especificó ningún valor en el combo, entonces se setea el valor por defecto Facturas A (001).
 * Este valor puede ser cambiado después de haber completado la factura.
 *  
 * Autor: Juan Manuel Martínez - jmmartinezsf@gmail.com
 * Versión 0.1 - septiembre de 2015
 * Para Libertya 15.03
 */

package com.jmm.exportaCITIRG3685.model;
 
import java.util.Properties;

import org.openXpertya.model.MLetraComprobante;
import org.openXpertya.model.PO;
import org.openXpertya.model.MDocType;
import org.openXpertya.plugin.MPluginPO;
import org.openXpertya.plugin.MPluginStatusPO;

public class MInvoice extends MPluginPO {

	public MInvoice(PO po, Properties ctx, String trxName, String aPackage) {
		super(po, ctx, trxName, aPackage);
	}
	
	public static final String AFIPDOCTYPE_NotasDeDebitoA = "02";
	/** Notas de Credito A = 03 */
	public static final String AFIPDOCTYPE_NotasDeCreditoA = "03";
	/** Recibos A = 04 */
	public static final String AFIPDOCTYPE_RecibosA = "04";
	/** Facturas B = 06 */
	public static final String AFIPDOCTYPE_FacturasB = "06";
	/** Notas de Debito B = 07 */
	public static final String AFIPDOCTYPE_NotasDeDebitoB = "07";
	/** Notas de Credito B = 08 */
	public static final String AFIPDOCTYPE_NotasDeCreditoB = "08";
	/** Recibos B = 09 */
	public static final String AFIPDOCTYPE_RecibosB = "09";
	/** Notas de Venta al contado B = 10 */
	public static final String AFIPDOCTYPE_NotasDeVentaAlContadoB = "10";
	/** Facturas A = 01 */
	public static final String AFIPDOCTYPE_FacturasA = "01";
	/** Notas de Venta al Contado A = 05 */
	public static final String AFIPDOCTYPE_NotasDeVentaAlContadoA = "05";
	/** Facturas C = 11 */
	public static final String AFIPDOCTYPE_FacturasC = "11";
	/** Notas de Debito C = 12 */
	public static final String AFIPDOCTYPE_NotasDeDebitoC = "12";
	/** Notas de Credito C = 13 */
	public static final String AFIPDOCTYPE_NotasDeCreditoC = "13";
	/** Recibos C = 15 */
	public static final String AFIPDOCTYPE_RecibosC = "15";
	/** Liquidación única comercial A = 27 */
	public static final String AFIPDOCTYPE_LiquidaciónÚnicaComercialA = "27";
	/** Liquidación única comercial B = 28 */
	public static final String AFIPDOCTYPE_LiquidaciónÚnicaComercialB = "28";
	/** Liquidación única comercial C = 29 */
	public static final String AFIPDOCTYPE_LiquidaciónÚnicaComercialC = "29";
	/** Liquidación primaria de granos = 33 */
	public static final String AFIPDOCTYPE_LiquidaciónPrimariaDeGranos = "33";
	/** Nota de Crédito Liq. única comercial B = 43 */
	public static final String AFIPDOCTYPE_NotaDeCréditoLiqÚnicaComercialB = "43";
	/** Nota de Crédito Liq. única comercial C = 44 */
	public static final String AFIPDOCTYPE_NotaDeCréditoLiqÚnicaComercialC = "44";
	/** Nota de Débito Liq. única comercial A = 45 */
	public static final String AFIPDOCTYPE_NotaDeDébitoLiqÚnicaComercialA = "45";
	/** Nota de Débito Liq. única comercial B = 46 */
	public static final String AFIPDOCTYPE_NotaDeDébitoLiqÚnicaComercialB = "46";
	/** Nota de Débito Liq. única comercial C = 47 */
	public static final String AFIPDOCTYPE_NotaDeDébitoLiqÚnicaComercialC = "47";
	/** Nota de Crédito Liq. única comercial A = 48 */
	public static final String AFIPDOCTYPE_NotaDeCréditoLiqÚnicaComercialA = "48";
	/** Facturas M = 51 */
	public static final String AFIPDOCTYPE_FacturasM = "51";
	/** Notas de Debito M = 52 */
	public static final String AFIPDOCTYPE_NotasDeDebitoM = "52";
	/** Notas de Credito M = 53 */
	public static final String AFIPDOCTYPE_NotasDeCreditoM = "53";
	/** Tique factura A = 81 */
	public static final String AFIPDOCTYPE_TiqueFacturaA = "81";
	/** Tique factura B = 82 */
	public static final String AFIPDOCTYPE_TiqueFacturaB = "82";
	/** Otros comprobantes - Notas de crédito = 90 */
	public static final String AFIPDOCTYPE_OtrosComprobantes_NotasDeCrédito = "90";
	/** Otros comprobantes = 99 */
	public static final String AFIPDOCTYPE_OtrosComprobantes = "99";
	
	public static String guessAFIPDocType(Properties ctx, boolean isSOTrx, int cLetraComprobanteId, int cDocTypeTargetId){
		
		if(cDocTypeTargetId==0)
			return null;		
		
		MDocType dt = MDocType.get(ctx, cDocTypeTargetId);

		String docBaseType = dt.getDocBaseType();
		String docTypeAFIP = dt.getdocsubtypecae();
		
		if(docTypeAFIP==null || docTypeAFIP.isEmpty()){	
			
			if (docBaseType.equals(MDocType.DOCTYPE_Retencion_Invoice) 
					|| docBaseType.equals(MDocType.DOCTYPE_Retencion_Receipt) 
					|| docBaseType.equals(MDocType.DOCTYPE_Retencion_InvoiceCustomer) 
					|| docBaseType.equals(MDocType.DOCTYPE_Retencion_ReceiptCustomer)
			){				
				return MInvoice.AFIPDOCTYPE_OtrosComprobantes;
			}else {				
				// Ayuda para determinar el codigo de comprobante segun AFIP
				// Comprobantes de compra, con letra asignada
				if (!isSOTrx && cLetraComprobanteId > 0){

					MLetraComprobante mLetraComprobante = new MLetraComprobante(
							ctx, cLetraComprobanteId, null);

					String letra = mLetraComprobante.getLetra();

					if(docBaseType.equals(MDocType.DOCBASETYPE_APInvoice)){
						// Facturas
						if(letra.equals("A"))
							return MInvoice.AFIPDOCTYPE_FacturasA;
						else if(letra.equals("B"))
							return MInvoice.AFIPDOCTYPE_FacturasB;
						else if(letra.equals("C"))
							return MInvoice.AFIPDOCTYPE_FacturasC;						
						else if(letra.equals("M"))
							return MInvoice.AFIPDOCTYPE_FacturasM;
						
					}else if(docBaseType.equals(MDocType.DOCBASETYPE_APCreditMemo)){
						// NCs
						if(letra.equals("A"))
							return MInvoice.AFIPDOCTYPE_NotasDeCreditoA;
						else if(letra.equals("B"))
							return MInvoice.AFIPDOCTYPE_NotasDeCreditoB;
						else if(letra.equals("C"))
							return MInvoice.AFIPDOCTYPE_NotasDeCreditoC;						
						else if(letra.equals("M"))
							return MInvoice.AFIPDOCTYPE_NotasDeCreditoM;						
					}
				}
			}
		}else{
			return docTypeAFIP;
		}
		
		return null;
	}
	
	public MPluginStatusPO preBeforeSave(PO po, boolean newRecord) {	
		LP_C_Invoice invoice = (LP_C_Invoice) po;
				
		if(invoice.getafipdoctype() == null || invoice.getafipdoctype().isEmpty()){
			String guessedAFIPDocType = guessAFIPDocType( invoice.getCtx(), 
					invoice.isSOTrx(), invoice.getC_Letra_Comprobante_ID(), invoice.getC_DocType_ID());
			
			if(guessedAFIPDocType!=null)
				invoice.setafipdoctype(guessedAFIPDocType);
		}
		
		return status_po;
	}

}
