package com.arcaneiceman.kraken.controller.io;

import com.arcaneiceman.kraken.domain.User;
import com.arcaneiceman.kraken.domain.enumerations.WorkerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class AccountIO {

    static final int PASSWORD_MIN_LENGTH = 4;

    static final int PASSWORD_MAX_LENGTH = 100;

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

    public static class Register {

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Request {

            @NotBlank
            @Email
            @Size(min = 1, max = 50)
            private String login;

            @NotBlank
            @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH)
            private String password;

            //@NotBlank
            private String firstName;

            //@NotBlank
            private String lastName;
        }

        @AllArgsConstructor
        public static class Response extends UserDTO {
            public Response(User user) {
                super(user);
            }
        }

    }

    public static class Authenticate {

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Request {

            @NotBlank
            @Email
            @Size(min = 1, max = 50)
            private String login;

            @NotBlank
            @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH)
            private String password;
        }

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Response {

            private String token;
        }

    }

    public static class Refresh {

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Response {

            private String token;
        }
    }

    public static class ResetPasswordInit {

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Request {

            @NotBlank
            private String mail;
        }

    }

    public static class ResetPasswordComplete {

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Request {

            @NotBlank
            private String key;

            @NotBlank
            @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH)
            private String newPassword;
        }

    }

    public static class ChangePassword {

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Request {

            @NotBlank
            @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH)
            private String oldPassword;

            @NotBlank
            @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH)
            private String newPassword;
        }
    }
}
