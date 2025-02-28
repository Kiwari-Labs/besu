/*
 * Copyright contributors to Besu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.consensus.qbft.adaptor;

import static org.assertj.core.api.Assertions.assertThat;

import org.hyperledger.besu.consensus.qbft.core.types.QbftBlockHeader;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.core.BlockHeaderTestFixture;

import org.junit.jupiter.api.Test;

class QbftBlockHeaderAdaptorTest {

  @Test
  void adaptsBesuBlockHeader() {
    BlockHeader header =
        new BlockHeaderTestFixture()
            .number(1)
            .timestamp(1000L)
            .coinbase(Address.ZERO)
            .buildHeader();
    QbftBlockHeader qbftBlockHeader = new QbftBlockHeaderAdaptor(header);

    assertThat(qbftBlockHeader.getNumber()).isEqualTo(1);
    assertThat(qbftBlockHeader.getTimestamp()).isEqualTo(1000L);
    assertThat(qbftBlockHeader.getHash()).isEqualTo(header.getHash());
    assertThat(qbftBlockHeader.getCoinbase()).isEqualTo(Address.ZERO);
  }
}
