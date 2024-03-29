package com.diva.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BOARD_ID")
    private Long id;

    @NotBlank
    private String boardName;

    @NotBlank
    private String boardCrawlingUrl;

    @NotBlank
    private String boardViewUrl;

    @NotNull
    private Boolean isThereNotice;

    @NotNull
    @OneToMany(mappedBy = "board")
    private List<Notification> notifications = new ArrayList<>();

    @NotNull
    @OneToMany(mappedBy = "board")
    private List<Article> articles = new ArrayList<>();

    @Builder
    protected Board(String boardName, String boardCrawlingUrl, String boardViewUrl, Boolean isThereNotice) {
        this.boardName = boardName;
        this.boardCrawlingUrl = boardCrawlingUrl;
        this.boardViewUrl = boardViewUrl;
        this.isThereNotice = isThereNotice;
    }
}
