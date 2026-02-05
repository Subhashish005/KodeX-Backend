package com.something.kodex_backend.token;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.something.kodex_backend.user.User;
import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Builder
public class Token {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  @Column(
    unique = true,
    length = 512
  )
  private String value;
  @Enumerated(EnumType.STRING)
  private TokenType type;
  private boolean revoked;

  // Token entity owns the relationship
  // as token can't exist without user.
  @ManyToOne
  @JoinColumn(
    name = "user_id"
  )
  // user will serialize tokens
  @JsonBackReference
  private User user;

}
