package com.arcaneiceman.kraken.controller.io;

import com.arcaneiceman.kraken.domain.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Maps the input and output of controller for a user
 */
public class UserIO {

    @Data
    @NoArgsConstructor
    private static class UserDTO {

        private Long id;

        @NotBlank
        @Email
        @Size(min = 1, max = 50)
        private String login;

        public UserDTO(User user) {
            this.id = user.getId();
            this.login = user.getLogin();
        }
    }

    public static class Get {
        @EqualsAndHashCode(callSuper = false)
        @Data
        @NoArgsConstructor
        public static class Response extends UserDTO {
            public Response(User user) {
                super(user);
            }
        }
    }


    public static class Create {
        @EqualsAndHashCode(callSuper = false)
        @Data
        @NoArgsConstructor
        public static class Request extends UserDTO {

        }
    }

    public static class Update {
        @EqualsAndHashCode(callSuper = false)
        @Data
        @NoArgsConstructor
        public static class Request extends UserDTO {

        }

        @EqualsAndHashCode(callSuper = false)
        @Data
        @NoArgsConstructor
        public static class Response extends UserDTO {
            public Response(User user) {
                super(user);
            }
        }
    }

    public static class Login {
        @Data
        @NoArgsConstructor
        public static class Request {
            @NotNull
            @Size(min = 1, max = 50)
            private String login;

            @NotNull
            //@Size(min = ManagedUserVM.PASSWORD_MIN_LENGTH, max = ManagedUserVM.PASSWORD_MAX_LENGTH)
            private String password;
        }
    }
}
