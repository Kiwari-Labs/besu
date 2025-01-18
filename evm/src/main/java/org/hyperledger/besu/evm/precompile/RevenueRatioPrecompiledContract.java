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

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.account.Account;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.frame.ExceptionalHaltReason;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;
import org.hyperledger.besu.evm.worldstate.WorldUpdater;

import javax.annotation.Nonnull;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RevenueRatioPrecompiledContract extends AbstractPrecompiledContract {
    private static final Logger LOG = LoggerFactory.getLogger(RevenueRatioPrecompiledContract.class);

    /** Ownable */
    private static final Bytes OWNER_SIGNATURE = Hash.keccak256(Bytes.of("owner()".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes INITIALIZED_SIGNATURE = Hash.keccak256(Bytes.of("initialized()".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes INITIALIZE_OWNER_SIGNATURE = Hash.keccak256(Bytes.of("initializeOwner(address)".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes TRANSFER_OWNERSHIP_SIGNATURE = Hash.keccak256(Bytes.of("transferOwnership(address)".getBytes(UTF_8))).slice(0, 4);

    /** RevenueRatio */
    // @TODO

    /** Storage Layout */
    // @TODO

    /** Returns */
    // @TODO

    public RevenueRatioPrecompiledContract(final GasCalculator gasCalculator) {
        super("RevenueRatioPrecompiledContract", gasCalculator);
    }

    @Override
    public long gasRequirement(final Bytes input) {
        final Bytes function = input.slice(0, 4);
        if (function.equals(OWNER_SIGNATURE) || 
            function.equals(INITIALIZED_SIGNATURE)) {
            // gas cost for read operation.
            return 1000;
        } else {
            // gas const for read operation.
            return 2000;
        }
    }

    @Nonnull
    @Override
    public PrecompileContractResult computePrecompile(final Bytes input, @Nonnull final MessageFrame messageFrame) {
        // @TODO
    }
}
