package com.arcaneiceman.kraken.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;

/**
 * A user.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Table(name = "users")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Email
    @Size(min = 1, max = 50)
    @Column(length = 50, unique = true, nullable = false)
    private String email;

    @Column
    private String firstName;

    @Column
    private String lastName;

    @JsonIgnore
    @NotNull
    @Size(min = 60, max = 60)
    @Column(length = 60)
    private String password;

    @Size(max = 20)
    @Column(length = 20)
    @JsonIgnore
    private String resetKey;

    @Column
    private Instant resetDate = null;

    @Column
    private Boolean active;

    @JsonIgnore
    @Column
    private String authority;

    public String getLogin() {
        return email;
    }

    public void setLogin(String login) {
        this.email = login;
    }

    @Override
    public String toString() {
        return "User{" +
                "email='" + getEmail() + "}";
    }
}
