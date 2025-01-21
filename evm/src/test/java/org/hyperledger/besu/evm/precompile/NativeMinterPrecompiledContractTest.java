/*
 * Copyright Advanced Info Services PCL.
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
package org.hyperledger.besu.evm.precompile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.FrontierGasCalculator;

import org.junit.jupiter.api.Test;
// import org.junit.jupiter.params.ParameterizedTest;
// import org.junit.jupiter.params.provider.Arguments;
// import org.junit.jupiter.params.provider.MethodSource;

class NativeMinterPrecompiledContractTest {
    /** @TODO */
    private final NativeMinterPrecompiledContract contract =
      new NativeMinterPrecompiledContract(new FrontierGasCalculator());

    NativeMinterPrecompiledContractTest() {}

    private final MessageFrame messageFrame = mock(MessageFrame.class);

    @Test
    void dryRunDetector() {
        assertThat(true)
        .withFailMessage("This test is here so gradle --dry-run executes this class")
        .isTrue();
    }
}
