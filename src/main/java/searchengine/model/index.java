package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Entity
@Table(name = "Indexes")
@Getter
@Setter

public class index {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY ,cascade = CascadeType.MERGE)
    @JoinColumn(name = "page_id", nullable = false)
    private page page;

    @ManyToOne(fetch = FetchType.LAZY ,cascade = CascadeType.MERGE)
    @JoinColumn(name = "lemma_id", nullable = false)
    private lemma lemma;

    @Column(name = "`rank`", columnDefinition = "FLOAT(7,4)", nullable = false)
    private float rank;

}
