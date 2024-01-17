package com.example.smartcontractfvss;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;


@DataType
@Data
@Accessors(chain = true)
public class ParticipantsQueryResult {

    @Property
    String key;

    @Property
    Participants participants;
}
