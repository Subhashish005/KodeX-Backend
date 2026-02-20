package com.something.kodex_backend.user;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.something.kodex_backend.project.Project;
import com.something.kodex_backend.token.Token;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
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
    nullable = false,
    length = 128
  )
  private String email;

  @Column(
    length = 128,
    nullable = false
  )
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(
    nullable = false
  )
  // every user has only one role for now
  private Role role;

  @Column(
    updatable = false,
    nullable = false
  )
  @CreationTimestamp
  private LocalDateTime createdAt;

  @OneToMany(
    mappedBy = "user",
    cascade = CascadeType.ALL
  )
  @JsonManagedReference
  // user is responsible to serialize tokens
  // important otherwise infinite recursion can happen
  private List<Token> refreshTokens;

  @OneToMany(
    mappedBy = "user",
    cascade = CascadeType.ALL
  )
  private List<Project> projects;

  // TODO: this will get implemented in future surely...
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

}