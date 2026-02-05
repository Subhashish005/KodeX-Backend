package com.something.kodex_backend.user;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.something.kodex_backend.token.Token;
import jakarta.persistence.*;
import lombok.*;
import org.checkerframework.common.aliasing.qual.Unique;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Builder
// postgres doesn't allow user as table name :/
@Table(name = "t_user")
public class User implements UserDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  @Column(
    unique = true,
    nullable = false,
    length = 32
  )
  private String username;
  @Column(
    unique = true,
    nullable = false
  )
  private String email;
  @Column(
    length = 128
  )
  private String password;
  // every user has only one role for now
  @Enumerated(EnumType.STRING)
  private Role role;

  @OneToMany(
    mappedBy = "user",
    cascade = CascadeType.ALL
  )
  // user is responsible to serialize tokens
  // important otherwise infinite recursion can happen
  @JsonManagedReference
  private List<Token> refreshTokens;

  // TODO: this will get implemented in future surely...
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

}
