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

public class NativeMinterPrecompiledContract extends AbstractPrecompiledContract {
    private static final Logger LOG = LoggerFactory.getLogger(StatefulPrecompiledContract.class);

    /** Ownable */
    private static final Bytes OWNER_SIGNATURE = Hash.keccak256(Bytes.of("owner()".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes INITIALIZED_SIGNATURE = Hash.keccak256(Bytes.of("initialized()".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes INITIALIZE_OWNER_SIGNATURE = Hash.keccak256(Bytes.of("initializeOwner(address)".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes TRANSFER_OWNERSHIP_SIGNATURE = Hash.keccak256(Bytes.of("transferOwnership(address)".getBytes(UTF_8))).slice(0, 4);

    /** NativeMinter */
    private static final Bytes MINT_SIGNATURE = Hash.keccak256(Bytes.of("mint(address,uint256)".getBytes(UTF_8))).slice(0, 4);

    /** Storage Layout */
    // @TODO slot 0 for owner

    public NativeMinterPrecompiledContract(final GasCalculator gasCalculator) {
        super("NativeMinterPrecompiledContract", gasCalculator);
    }

    // private Bytes owner(final MutableAccount contract) {
    //      return 
    // }

    // private Bytes initialized(final MutableAccount contract) {
    //      return 
    // }

    // private Bytes initializeOwner(final MutableAccount contract, MessageFrame messageFrame) {
    //      return 
    // }

    // private Bytes transferOwnership(final MutableAccount contract, MessageFrame messageFrame) {
    //      // check msg.sender is owner
    //      return 
    // }

    // private Bytes mint(final MutableAccount to, MessageFrame messageFrame) {
    //     // check msg.sender is owner
    //     to.incrementBalance(value);
    //     return
    // }

    @Override
    public long gasRequirement(final Bytes input) {
        final Bytes function = input.slice(0, 4);
        if (function.equals(OWNER_SIGNATURE) || function.equals(INITIALIZED_SIGNATURE)) {
            return 1000;
        } else {
            return 2300;
        }
    }

    @Nonnull
    @Override
    public PrecompileContractResult computePrecompile(final Bytes input, @Nonnull final MessageFrame messageFrame) {
        // @TODO try catch style.
        if (input.isEmpty()) {
            return PrecompileContractResult.halt(
          null, Optional.of(ExceptionalHaltReason.PRECOMPILE_ERROR));
        } else { 
            final Bytes function = input.slice(0, 4);
            final WorldUpdater worldUpdater = messageFrame.getWorldUpdater();
            final MutableAccount nativeMinter = worldUpdater.getOrCreate(Address.NATIVE_MINTER);
            if (function.equals(OWNER_SIGNATURE)) {
                return PrecompileContractResult.success(
                    owner(nativeMinter)
                    );
            } 
            else if (function.equals(INITIALIZED_SIGNATURE)) {
                return PrecompileContractResult.success(
                    initialized(nativeMinter)
                    );
            }
            else if (function.equals(INITIALIZE_OWNER_SIGNATURE)) {
                return PrecompileContractResult.success(
                    initializeOwner(nativeMinter, input)
                    );
            }
            else if (function.equals(TRANSFER_OWNERSHIP_SIGNATURE)) {
                return PrecompileContractResult.success(
                    transferOwnership(nativeMinter, input)
                    );
            }
            else if (function.equals(MINT_SIGNATURE)) {
                return PrecompileContractResult.success(
                    mint(nativeMinter, input)
                    );
            } else {
                LOG.info("Failed interface not found");
                return PrecompileContractResult.halt(null, Optional.of(ExceptionalHaltReason.PRECOMPILE_ERROR));
            }
        }
    }
}