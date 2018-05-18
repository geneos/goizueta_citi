/** Modelo Generado - NO CAMBIAR MANUALMENTE - Disytel */
package com.jmm.exportaCITIRG3685.model;
import org.openXpertya.model.*;
import java.util.logging.Level;
 import java.util.*;
import java.sql.*;
import java.math.*;
import org.openXpertya.util.*;
/** Modelo Generado por C_Invoice
 *  @author Comunidad de Desarrollo Libertya*         *Basado en Codigo Original Modificado, Revisado y Optimizado de:*         * Jorg Janke 
 *  @version  - 2015-09-30 17:22:02.311 */
public class LP_C_Invoice extends org.openXpertya.model.MInvoice
{
/** Constructor estándar */
public LP_C_Invoice (Properties ctx, int C_Invoice_ID, String trxName)
{
super (ctx, C_Invoice_ID, trxName);
/** if (C_Invoice_ID == 0)
{
}
 */
}
/** Load Constructor */
public LP_C_Invoice (Properties ctx, ResultSet rs, String trxName)
{
super (ctx, rs, trxName);
}
public String toString()
{
StringBuffer sb = new StringBuffer ("LP_C_Invoice[").append(getID()).append("]");
return sb.toString();
}
public static final int AFIPDOCTYPE_AD_Reference_ID = MReference.getReferenceID("DocSubTypeCae");
/** Notas de Debito A = 02 */
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
/** Set Tipo documento CITI */
public void setafipdoctype (String afipdoctype)
{
if (afipdoctype == null || afipdoctype.equals("02") || afipdoctype.equals("03") || afipdoctype.equals("04") || afipdoctype.equals("06") || afipdoctype.equals("07") || afipdoctype.equals("08") || afipdoctype.equals("09") || afipdoctype.equals("10") || afipdoctype.equals("01") || afipdoctype.equals("05") || afipdoctype.equals("11") || afipdoctype.equals("12") || afipdoctype.equals("13") || afipdoctype.equals("15") || afipdoctype.equals("27") || afipdoctype.equals("28") || afipdoctype.equals("29") || afipdoctype.equals("33") || afipdoctype.equals("43") || afipdoctype.equals("44") || afipdoctype.equals("45") || afipdoctype.equals("46") || afipdoctype.equals("47") || afipdoctype.equals("48") || afipdoctype.equals("51") || afipdoctype.equals("52") || afipdoctype.equals("53") || afipdoctype.equals("81") || afipdoctype.equals("82") || afipdoctype.equals("90") || afipdoctype.equals("99"));
 else throw new IllegalArgumentException ("afipdoctype Invalid value - Reference = AFIPDOCTYPE_AD_Reference_ID - 02 - 03 - 04 - 06 - 07 - 08 - 09 - 10 - 01 - 05 - 11 - 12 - 13 - 15 - 27 - 28 - 29 - 33 - 43 - 44 - 45 - 46 - 47 - 48 - 51 - 52 - 53 - 81 - 82 - 90 - 99");
if (afipdoctype != null && afipdoctype.length() > 2)
{
log.warning("Length > 2 - truncated");
afipdoctype = afipdoctype.substring(0,2);
}
set_Value ("afipdoctype", afipdoctype);
}
/** Get Tipo documento CITI */
public String getafipdoctype() 
{
return (String)get_Value("afipdoctype");
}
}
