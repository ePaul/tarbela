package org.zalando.tarbela.config.util;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProducerInteractorContainer {
    private List<ProducerInteractor> producerInteractors;

}
