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

import static java.nio.charset.StandardCharsets.UTF_8;

import org.hyperledger.besu.crypto.Hash;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.frame.ExceptionalHaltReason;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.worldstate.WorldUpdater;

import java.util.Optional;
import javax.annotation.Nonnull;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TreasuryRegistryPrecompiledContract extends AbstractPrecompiledContract {
  private static final Logger LOG =
      LoggerFactory.getLogger(TreasuryRegistryPrecompiledContract.class);

  /** Ownable */
  private static final Bytes OWNER_SIGNATURE =
      Hash.keccak256(Bytes.of("owner()".getBytes(UTF_8))).slice(0, 4);

  private static final Bytes INITIALIZED_SIGNATURE =
      Hash.keccak256(Bytes.of("initialized()".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes INITIALIZE_OWNER_SIGNATURE =
      Hash.keccak256(Bytes.of("initializeOwner(address)".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes TRANSFER_OWNERSHIP_SIGNATURE =
      Hash.keccak256(Bytes.of("transferOwnership(address)".getBytes(UTF_8))).slice(0, 4);

  /** TreasuryRegistry */
  private static final Bytes TREASURY_AT_SIGNATURE =
      Hash.keccak256(Bytes.of("treasuryAt()".getBytes(UTF_8))).slice(0, 4);

  private static final Bytes SET_TREASURY_SIGNATURE =
      Hash.keccak256(Bytes.of("setTreasury(address)".getBytes(UTF_8))).slice(0, 4);

  /** Storage Layout */
  private static final UInt256 INIT_SLOT = UInt256.ZERO;

  private static final UInt256 OWNER_SLOT = UInt256.ONE;

  private static final UInt256 TREASURY_SLOT = UInt256.valueOf(2L);

  /** Returns */
  private static final Bytes FALSE =
      Bytes.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000000");

  private static final Bytes TRUE =
      Bytes.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000001");

  /** Modifier */
  private Bytes onlyOwner(final MutableAccount contract, final Address senderAddress) {
    final Address storedOwner = Address.wrap(contract.getStorageValue(OWNER_SLOT));
    if (storedOwner.equals(senderAddress)) {
      return TRUE;
    } else {
      return FALSE;
    }
  }

  private Bytes owner(final MutableAccount contract) {
    return contract.getStorageValue(OWNER_SLOT);
  }

  private Bytes initialized(final MutableAccount contract) {
    return contract.getStorageValue(INIT_SLOT);
  }

  private Bytes initializeOwner(
      final MutableAccount contract, final Address senderAddress, final Bytes calldata) {
    if (initialized(contract).equals(TRUE)) {
      return FALSE;
    } else {
      contract.setStorageValue(INIT, TRUE);
      // extract/slice address from messageFrame
      // contract.setStorageValue(OWNER_SLOT, initialOwner);
      return TRUE;
    }
  }

  private Bytes transferOwnership(
      final MutableAccount contract, final Address senderAddress, final Bytes calldata) {
    if (onlyOwner(contract, senderAddress).equals(TRUE)) {
      return FALSE;
    } else {
      // extract/slice address from messageFrame
      // contract.setStorageValue(OWNER_SLOT, neOwner);
      return TRUE;
    }
  }

  private Bytes treasuryAt(final MutableAccount contract) {
    return contract.getStorageValue(TREASURY_SLOT);
  }

  private Bytes setTreasury(
      final MutableAccount contract, final Address senderAddress, final Bytes calldata) {
    if (onlyOwner(contract, senderAddress).equals(TRUE)) {
      return FALSE;
    } else {
      // extract/slice address from messageFrame
      // contract.setStorageValue(OWNER, neOwner);
      return TRUE;
    }
  }

  @Override
  public long gasRequirement(final Bytes input) {
    final Bytes function = input.slice(0, 4);
    if (function.equals(SET_TREASURY_SIGNATURE)) {
      // gas cost for write operation.
      return 2000;
    } else {
      // gas const for read operation.
      return 1000;
    }
  }

  @Nonnull
  @Override
  public PrecompileContractResult computePrecompile(
      final Bytes input, @Nonnull final MessageFrame messageFrame) {
    // @TODO try catch style.
    if (input.isEmpty()) {
      return PrecompileContractResult.halt(
          null, Optional.of(ExceptionalHaltReason.PRECOMPILE_ERROR));
    } else {
      final Bytes function = input.slice(0, 4);
      final Bytes calldata = input.slice(4);
      final WorldUpdater worldUpdater = messageFrame.getWorldUpdater();
      final Address senderAddress = messageFrame.getSenderAddress();
      final MutableAccount precompile = worldUpdater.getOrCreate(Address.TREASURY);
      if (function.equals(OWNER_SIGNATURE)) {
        return PrecompileContractResult.success(owner(precompile));
      } else if (function.equals(INITIALIZED_SIGNATURE)) {
        return PrecompileContractResult.success(initialized(precompile));
      } else if (function.equals(INITIALIZE_OWNER_SIGNATURE)) {
        return PrecompileContractResult.success(initializeOwner(precompile, calldata));
      } else if (function.equals(TRANSFER_OWNERSHIP_SIGNATURE)) {
        return PrecompileContractResult.success(
            transferOwnership(precompile, senderAddress, calldata));
      } else if (function.equals(TREASURY_AT_SIGNATURE)) {
        return PrecompileContractResult.success(treasuryAt(precompile));
      } else if (function.equals(SET_TREASURY_SIGNATURE)) {
        return PrecompileContractResult.success(setTreasury(precompile, senderAddress, calldata));
      } else {
        // @TODO logging the invalid function signature.
        LOG.info("Failed interface not found");
        return PrecompileContractResult.halt(
            null, Optional.of(ExceptionalHaltReason.PRECOMPILE_ERROR));
      }
    }
  }
}
