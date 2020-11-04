package cz.cas.lib.arclib.security.authorization.logic;

import cz.cas.lib.arclib.security.authorization.data.Permissions;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ValidPermissionValidator implements ConstraintValidator<ValidPermission, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;
        return Permissions.ALL_PERMISSIONS.contains(value);
    }
}
