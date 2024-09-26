package cz.cas.lib.core.security.password;

import org.passay.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@ConditionalOnProperty(prefix = "security.local", name = "enabled", havingValue = "true")
@Service
public class GoodPasswordGenerator {
    private Integer minPasswordLength;

    private Boolean requireDigit;

    private Boolean requireAlphabet;

    private List<Rule> rules;
    private List<CharacterRule> characterRules;

    @Autowired
    public GoodPasswordGenerator(@Value("${security.local.password.length:8}") Integer minPasswordLength,
                                 @Value("${security.local.password.digit:true}") Boolean requireDigit,
                                 @Value("${security.local.password.alphabet:true}") Boolean requireAlphabet) {
        this.minPasswordLength = minPasswordLength;
        this.requireDigit = requireDigit;
        this.requireAlphabet = requireAlphabet;

        LengthRule lengthRule = new LengthRule(minPasswordLength, Integer.MAX_VALUE);
        WhitespaceRule whitespaceRule = new WhitespaceRule();

        // control allowed characters
        characterRules = new ArrayList<>();
        if (requireDigit) {
            characterRules.add(new CharacterRule(EnglishCharacterData.Digit, 1));
        }

        if (requireAlphabet) {
            characterRules.add(new CharacterRule(EnglishCharacterData.Alphabetical, 1));
        }

        CharacterCharacteristicsRule charRule = new CharacterCharacteristicsRule();
        charRule.getRules().addAll(characterRules);
        charRule.setNumberOfCharacteristics(charRule.getRules().size());

        // group all rules together in a List
        rules = new ArrayList<>();
        rules.add(lengthRule);
        rules.add(whitespaceRule);
        rules.add(charRule);
    }

    public boolean isValid(String password) {
        PasswordValidator validator = new PasswordValidator(rules);
        PasswordData passwordData = new PasswordData(password);

        RuleResult result = validator.validate(passwordData);
        return result.isValid();
    }

    public String generate() {
        PasswordGenerator generator = new PasswordGenerator();
        return generator.generatePassword(minPasswordLength, characterRules);
    }
}
