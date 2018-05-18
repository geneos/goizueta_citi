/** Modelo Generado - NO CAMBIAR MANUALMENTE - Disytel */
package com.jmm.exportaCITIRG3685.model;
import org.openXpertya.model.*;
import java.util.logging.Level;
 import java.util.*;
import java.sql.*;
import java.math.*;
import org.openXpertya.util.*;
/** Modelo Generado por C_Tax
 *  @author Comunidad de Desarrollo Libertya*         *Basado en Codigo Original Modificado, Revisado y Optimizado de:*         * Jorg Janke 
 *  @version  - 2015-09-30 17:22:04.499 */
public class LP_C_Tax extends org.openXpertya.model.MTax
{
/** Constructor estándar */
public LP_C_Tax (Properties ctx, int C_Tax_ID, String trxName)
{
super (ctx, C_Tax_ID, trxName);
/** if (C_Tax_ID == 0)
{
}
 */
}
/** Load Constructor */
public LP_C_Tax (Properties ctx, ResultSet rs, String trxName)
{
super (ctx, rs, trxName);
}
public String toString()
{
StringBuffer sb = new StringBuffer ("LP_C_Tax[").append(getID()).append("]");
return sb.toString();
}
public static final int CITIRG3685_AD_Reference_ID = MReference.getReferenceID("CatCITIRG3685");
/** Crédito o Débito fiscal (IVA) = CDF */
public static final String CITIRG3685_CréditoODébitoFiscalIVA = "CDF";
/** Importes exentos = EXE */
public static final String CITIRG3685_ImportesExentos = "EXE";
/** Percepciones de IVA = PIV */
public static final String CITIRG3685_PercepcionesDeIVA = "PIV";
/** Percepciones de Ingresos Brutos = PIB */
public static final String CITIRG3685_PercepcionesDeIngresosBrutos = "PIB";
/** Percepciones de impuestos nacionales = PNC */
public static final String CITIRG3685_PercepcionesDeImpuestosNacionales = "PNC";
/** Percepciones de impuestos municipales = PMN */
public static final String CITIRG3685_PercepcionesDeImpuestosMunicipales = "PMN";
/** Otros impuestos = OTR */
public static final String CITIRG3685_OtrosImpuestos = "OTR";
/** Set Categoría CITI RG3685 */
public void setcitirg3685 (String citirg3685)
{
if (citirg3685 == null || citirg3685.equals("CDF") || citirg3685.equals("EXE") || citirg3685.equals("PIV") || citirg3685.equals("PIB") || citirg3685.equals("PNC") || citirg3685.equals("PMN") || citirg3685.equals("OTR"));
 else throw new IllegalArgumentException ("citirg3685 Invalid value - Reference = CITIRG3685_AD_Reference_ID - CDF - EXE - PIV - PIB - PNC - PMN - OTR");
if (citirg3685 != null && citirg3685.length() > 3)
{
log.warning("Length > 3 - truncated");
citirg3685 = citirg3685.substring(0,3);
}
set_Value ("citirg3685", citirg3685);
}
/** Get Categoría CITI RG3685 */
public String getcitirg3685() 
{
return (String)get_Value("citirg3685");
}
}
