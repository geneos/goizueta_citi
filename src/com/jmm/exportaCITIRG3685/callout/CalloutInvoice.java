package com.jmm.exportaCITIRG3685.callout;

import java.util.Properties;

import org.openXpertya.model.CalloutEngine;
import org.openXpertya.model.MField;
import org.openXpertya.model.MTab;
import org.openXpertya.util.CLogger;
import org.openXpertya.util.Env;

import com.jmm.exportaCITIRG3685.model.MInvoice;

public class CalloutInvoice extends CalloutEngine {
	private static CLogger s_log = CLogger.getCLogger(CalloutInvoice.class);	
		
	public String postC_DocType_ID( Properties ctx,int WindowNo,MTab mTab,MField mField,Object value ){
		return fixAFIPDocType(ctx, WindowNo, mTab);
	}	
	
	public String postC_DocTypeTarget_ID( Properties ctx,int WindowNo,MTab mTab,MField mField,Object value ){
		return fixAFIPDocType(ctx, WindowNo, mTab);
	}
	
	public String postC_Letra_Comprobante_ID( Properties ctx,int WindowNo,MTab mTab,MField mField,Object value ){
		return fixAFIPDocType(ctx, WindowNo, mTab);
	}
	
	private String fixAFIPDocType(Properties ctx, int WindowNo, MTab mTab){
		if(!mTab.isInserting())
			return "";

		String state = "";
		boolean isSOTrx = Env.getContext(ctx, WindowNo, "IsSOTrx").equals("Y");		
		int cLetraComprobanteId = Env.getContextAsInt(ctx, WindowNo, "C_Letra_Comprobante_ID");
		int cDocTypeTargetId = Env.getContextAsInt(ctx, WindowNo, "C_DocTypeTarget_ID");
		
		if(cDocTypeTargetId==0)
			return state;
		
		String guessedAFIPDocType = MInvoice.guessAFIPDocType( ctx, isSOTrx, cLetraComprobanteId, cDocTypeTargetId);
		
		if(guessedAFIPDocType!=null)
			setAfipDocType(mTab, guessedAFIPDocType);

		return state;
	}
	
	private void setAfipDocType(MTab mTab, String afipDocType){
		if(afipDocType==null)
			return;
		s_log.finest("setting afipdoctype = " + afipDocType);
		try{
			mTab.setValue("afipdoctype", afipDocType);
		}catch(Exception e){
			s_log.warning(e.getMessage());
		}
	}	
}
