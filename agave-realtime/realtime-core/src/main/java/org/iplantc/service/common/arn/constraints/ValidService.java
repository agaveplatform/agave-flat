package org.iplantc.service.common.arn.constraints;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;
import org.iplantc.service.common.arn.validation.ValidServiceValidator;

@NotNull
@NotEmpty
@Target( { METHOD, FIELD, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = ValidServiceValidator.class)
@Documented
public @interface ValidService {

    String message() default "{org.iplantc.service.common.arn.validation.ValidService.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}