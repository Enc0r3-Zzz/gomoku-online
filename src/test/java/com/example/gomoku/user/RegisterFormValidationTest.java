package com.example.gomoku.user;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterFormValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidForm() {
        assertThat(validator.validate(validForm())).isEmpty();
    }

    @Test
    void rejectsUsernameThatIsTooShort() {
        RegisterForm form = validForm();
        form.setUsername("ab");

        assertThat(validator.validate(form))
                .anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    void rejectsUsernameWithIllegalCharacter() {
        RegisterForm form = validForm();
        form.setUsername("alice!");

        assertThat(validator.validate(form))
                .anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    void rejectsPasswordWithoutDigit() {
        RegisterForm form = validForm();
        form.setPassword("onlyletters");

        assertThat(validator.validate(form))
                .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void rejectsPasswordThatIsTooShort() {
        RegisterForm form = validForm();
        form.setPassword("a1");

        assertThat(validator.validate(form))
                .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    private RegisterForm validForm() {
        RegisterForm form = new RegisterForm();
        form.setUsername("alice");
        form.setNickname("Alice");
        form.setPassword("gomoku123");
        return form;
    }
}
