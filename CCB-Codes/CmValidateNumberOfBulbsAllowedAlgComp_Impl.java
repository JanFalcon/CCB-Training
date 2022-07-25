package com.splwg.cm.domain.admin.serviceAgreementType.algorithm.saActivation;

import java.math.BigInteger;

import com.splwg.base.api.Query;
import com.splwg.base.api.QueryResultRow;
import com.splwg.base.domain.common.characteristicType.CharacteristicType;
import com.splwg.ccb.api.lookup.AdjustmentStatusLookup;
import com.splwg.ccb.domain.adjustment.adjustment.entity.Adjustment;
import com.splwg.ccb.domain.adjustment.adjustment.entity.Adjustment_DTO;
import com.splwg.ccb.domain.admin.adjustmentType.entity.AdjustmentType;
import com.splwg.ccb.domain.admin.serviceAgreementType.algorithm.saActivation.SaTypeSaActivationAlgorithmSpot;
import com.splwg.ccb.domain.customerinfo.serviceAgreement.entity.ServiceAgreement;
import com.splwg.cm.domain.customMessages.CustomMessageRepository;
import com.splwg.shared.logging.Logger;
import com.splwg.shared.logging.LoggerFactory;

/**
 * @author cissys
 *
@AlgorithmComponent (softParameters = { @AlgorithmSoftParameter (entityName = adjustmentType, name = excessBulbAdjustmentType, required = true, type = entity)
 *            , @AlgorithmSoftParameter (name = maximumNumberOfBulbsAllowed, required = true, type = integer)
 *            , @AlgorithmSoftParameter (entityName = characteristicType, name = numberOfBulbsCharacteristicType, required = true, type = entity)})
 */
public class CmValidateNumberOfBulbsAllowedAlgComp_Impl extends
		CmValidateNumberOfBulbsAllowedAlgComp_Gen implements
		SaTypeSaActivationAlgorithmSpot {

	/* Hard Parameters */
	// Input
	private ServiceAgreement inputServiceAgreement;
	
	/* Soft Parameters */
	private CharacteristicType paramNumberOfBulbsCharacteristicType; //CM-BULB
	private BigInteger paramMaximumNumberOfBulbsAllowed; //5
	private AdjustmentType paramExcessBulbAdjustmentType; //CM-ADBULB
	
	// Work Variables
	private Logger logger = LoggerFactory.getLogger(CmValidateNumberOfBulbsAllowedAlgComp_Impl.class);
	private int retrievedCharacteristicValue = 0;
	
	
	protected void extraSoftParameterValidations(boolean forAlgorithmValidation) {
		
		paramNumberOfBulbsCharacteristicType = getNumberOfBulbsCharacteristicType();
		paramMaximumNumberOfBulbsAllowed = getMaximumNumberOfBulbsAllowed();
		paramExcessBulbAdjustmentType = getExcessBulbAdjustmentType();
		
		if(paramMaximumNumberOfBulbsAllowed.compareTo(BigInteger.ZERO) <= 0){
			addError(CustomMessageRepository.invalidMaximumNumberOfBulbs());
		}
		
		if(!validateCharacteristicTypeIsForPremise()){		
			addError(CustomMessageRepository.notPremiseCharacteristicType());
		}

	}

	public void invoke() {
		logger.info("=================================" + paramNumberOfBulbsCharacteristicType.getCharacteristicType());
		
		if(!validateRetrievedCharacteristicValue()){
			addError(CustomMessageRepository.adhocValueNotAnInteger());
		}
		
		if(retrievedCharacteristicValue > paramMaximumNumberOfBulbsAllowed.intValue()){
			logger.info("JMFB DEBUG: retrieved Number of Bulbs : " + retrievedCharacteristicValue);
            Adjustment_DTO adjDTO = new Adjustment_DTO();
            adjDTO.setAdjustmentTypeId(paramExcessBulbAdjustmentType.getId());
            logger.info("JMFB DEBUG: 1");
            adjDTO.setServiceAgreementId(inputServiceAgreement.getId());
            logger.info("JMFB DEBUG: 2");
            adjDTO.setBaseAmount(paramExcessBulbAdjustmentType.getId().getEntity().getDefaultAmount());
            logger.info("JMFB DEBUG: 3");
            adjDTO.setCreDt(getProcessDateTime().getDate());
            logger.info("JMFB DEBUG: 4");
            adjDTO.setAdjustmentStatus(AdjustmentStatusLookup.constants.INCOMPLETE);
            logger.info("JMFB DEBUG: 5");
            Adjustment adj = adjDTO.newEntity();
            logger.info("JMFB DEBUG: 6");
            adj.generate(getProcessDateTime().getDate(), getProcessDateTime().getDate());
            logger.info("JMFB DEBUG: 7");
            adj.freeze(getProcessDateTime().getDate());
            logger.info("JMFB DEBUG: TASK FAILED SUCCESSFULLY!");
		}
	}
	 
	public boolean validateRetrievedCharacteristicValue(){
		try{
			retrievedCharacteristicValue = Integer.valueOf(getNumberOfBulbsCharacteristicValue());
			return true;
		}
		catch(Exception e){
			return false;
		}
	}
	
	private String getNumberOfBulbsCharacteristicValue(){
		StringBuilder sb = new StringBuilder();
		sb.append(" FROM  ServiceAgreement SA, ");
		sb.append(" PremiseCharacteristic PC ");
		sb.append(" WHERE ");
		sb.append("  SA.premiseId = PC.id.premise.id ");
		sb.append(" AND ");
		sb.append("  PC.id.characteristicType.id = :characteristicType ");
		
		Query<String> query = createQuery(sb.toString());
		
		query.bindId("characteristicType", paramNumberOfBulbsCharacteristicType.getId());
		query.addResult("characteristicValue", "PC.adhocCharacteristicValue");
		
//		return Integer.valueOf(adhocValue);
		return query.firstRow();
		
		/* ~~~~~ SQL QUERY ~~~~~
		SELECT 
		pc.adhoc_char_val
		--*
		FROM 
		ci_sa sa
		, ci_prem_char pc
		WHERE
		sa.char_prem_id =  pc.prem_id
		AND pc.char_type_cd = 'CM-BULB'
		;
		*/
	}
	
	
	// Validate if Characteristic type is for Premise Identity
	public Boolean  validateCharacteristicTypeIsForPremise(){
		StringBuilder sb = new StringBuilder();
		
		sb.append(" FROM PremiseCharacteristic PC ");
		sb.append(" WHERE ");
		sb.append("  PC.id.characteristicType.id = :characteristicType  ");
		
		Query<QueryResultRow> query = createQuery(sb.toString());
		query.bindId("characteristicType", paramNumberOfBulbsCharacteristicType.getId());
		
		query.addResult("char_type_cd", "PC.id.characteristicType.id");
		
//		Returns true if query results is greater than zero elese return false
		logger.info(query.list().size() + "LOG HERE ====================================");
		return query.list().size() > 0;
		
		/* ~~~~~ SQL QUERY ~~~~~
		SELECT * FROM CI_PREM_CHAR WHERE CHAR_TYPE_CD = :characteristicType;
		*/
	}
	
	public void setServiceAgreement(ServiceAgreement inputServiceAgreement) {
		this.inputServiceAgreement = inputServiceAgreement;
	}

}
