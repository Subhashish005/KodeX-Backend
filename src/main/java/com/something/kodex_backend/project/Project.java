package com.something.kodex_backend.project;

import com.something.kodex_backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Entity
@Table(name="t_project")
public class Project {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(
    unique = true,
    nullable = false,
    length = 128
  )
  private String name;

  @Column(
    unique = true,
    nullable = false
  )
  private String googleDriveId;

  @Column(
    nullable = false,
    updatable = false
  )
  private Instant createdAt;

  @Column(
    nullable = false
  )
  private Instant modifiedAt;

  @Column(
    nullable = false
  )
  private String language;

  @ManyToOne
   @JoinColumn(
     name = "user_id"
   )
  private User user;

}