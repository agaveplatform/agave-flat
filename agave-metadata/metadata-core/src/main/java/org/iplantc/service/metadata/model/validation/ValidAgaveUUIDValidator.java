package org.iplantc.service.metadata.model.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.iplantc.service.common.exceptions.UUIDException;
import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;
import org.iplantc.service.metadata.model.validation.constraints.ValidAgaveUUID;

import java.util.Locale;

public class ValidAgaveUUIDValidator implements ConstraintValidator<ValidAgaveUUID, Object> {

    private UUIDType uuidType;

    @Override
    public void initialize(final ValidAgaveUUID constraintAnnotation) {
        uuidType = constraintAnnotation.type();
    }

    @Override
    public boolean isValid(Object target, final ConstraintValidatorContext constraintContext) {
        
        boolean isValid = false;
        if (target == null) {
            return true;
        }
        
        AgaveUUID uuid = null;
        
        if (target instanceof String) {
            try {
                uuid = new AgaveUUID((String) target);
            } catch (UUIDException e) {
                constraintContext.disableDefaultConstraintViolation();
                constraintContext.buildConstraintViolationWithTemplate( 
                            "Invalid uuid value")
                        .addConstraintViolation();
            }
        }
        
        if (isValidUUIDType(uuid)) {
            try {
                uuid.getObjectReference();
                isValid = true;
            } catch (Exception e) {
                constraintContext.disableDefaultConstraintViolation();
                constraintContext.buildConstraintViolationWithTemplate( 
                            "No resource found matching the given uuid " + uuid.toString())
                        .addConstraintViolation();
            }
            
            // check permissions here
            
        } else {
            constraintContext.disableDefaultConstraintViolation();
            constraintContext.buildConstraintViolationWithTemplate( 
                         uuid.toString() +
                        " is not a valid " + this.uuidType.name().toLowerCase() + " id" )
                    .addConstraintViolation();
        }
        
        return isValid;
        
        
    }
    
    /**
     * Verifies the {@link UUIDType}, if provided, is valid for the value of
     * the annotation target.
     * @param uuid the target value of this annotation resolved to an {@link AgaveUUID}
     * @return true if no {@link UUIDType} was provided in the annotation or the types match. false otherwise.
     */
    private boolean isValidUUIDType(AgaveUUID uuid) {
        if (this.uuidType != null && this.uuidType != uuid.getResourceType()) {
            return false;
        }
        
        return true;
    }
}
