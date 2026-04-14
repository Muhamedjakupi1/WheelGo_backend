package com.wheelGo.model.vehicles;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
@Getter @Setter

public class vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;


}
