/*
 * Copyright contributors to Hyperledger Besu.
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

import static java.nio.charset.StandardCharsets.UTF_8;

import org.hyperledger.besu.crypto.Hash;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.frame.ExceptionalHaltReason;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;
import org.hyperledger.besu.evm.worldstate.WorldUpdater;

import java.util.Optional;
import javax.annotation.Nonnull;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatefulPrecompiledContract extends AbstractPrecompiledContract {

  private static final Logger LOG = LoggerFactory.getLogger(StatefulPrecompiledContract.class);

  private static final UInt256 STORAGE_VALUE =
      UInt256.ZERO; // declaration storage slot zero for store value;
  private static final Bytes GET_SIGNATURE =
      Hash.keccak256(Bytes.of("get()".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes SET_SIGNATURE =
      Hash.keccak256(Bytes.of("set(uint256)".getBytes(UTF_8))).slice(0, 4);
  private static final Address STORAGE_CONTRACT_ADDRESS =
      Address.fromHexString("0x0100000000000000000000000000000000000001");

  public StatefulPrecompiledContract(final GasCalculator gasCalculator) {
    super("StatefulPrecompiledContract", gasCalculator);
  }

  @Override
  public long gasRequirement(final Bytes input) {
    final Bytes function = input.slice(0, 4);
    if (function.equals(SET_SIGNATURE)) {
      return 2000;
    } else {
      return 1000;
    }
  }

  @Nonnull
  @Override
  public PrecompileContractResult computePrecompile(
      final Bytes input, @Nonnull final MessageFrame messageFrame) {
    if (input.isEmpty()) {
      return PrecompileContractResult.halt(
          null, Optional.of(ExceptionalHaltReason.PRECOMPILE_ERROR));
    } else {
      // function selector.
      final Bytes function = input.slice(0, 4);
      // slicing for payload data.
      final Bytes payload = input.slice(4);
      final WorldUpdater worldUpdater = messageFrame.getWorldUpdater();
      final MutableAccount mutableAccount = worldUpdater.getOrCreate(STORAGE_CONTRACT_ADDRESS);
      final Bytes state = mutableAccount.getStorageValue(STORAGE_VALUE);
      if (function.equals(GET_SIGNATURE)) {
        LOG.trace("Latest state is {}", state);
        return PrecompileContractResult.success(state);
      } else if (function.equals(SET_SIGNATURE)) {
        final UInt256 payloadAsUInt256 = UInt256.fromBytes(Bytes32.leftPad(payload));
        // NOTE: you need to initialized the balance of address to 0x1 in genesis.json first.
        mutableAccount.setStorageValue(STORAGE_VALUE, payloadAsUInt256);
        LOG.trace("State update from {} to {}", state, payloadAsUInt256);
        return PrecompileContractResult.success(Bytes.EMPTY);
      } else {
        LOG.trace("Failed interface not found");
        return PrecompileContractResult.halt(
            null, Optional.of(ExceptionalHaltReason.PRECOMPILE_ERROR));
      }
    }
  }
}
