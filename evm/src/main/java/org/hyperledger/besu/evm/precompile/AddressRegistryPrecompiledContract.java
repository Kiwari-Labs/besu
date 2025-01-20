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
package org.hyperledger.besu.evm.precompile;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.frame.ExceptionalHaltReason;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;
import org.hyperledger.besu.evm.worldstate.WorldUpdater;

import java.util.Optional;
import javax.annotation.Nonnull;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddressRegistryPrecompiledContract extends AbstractPrecompiledContract {
  private static final Logger LOG =
      LoggerFactory.getLogger(AddressRegistryPrecompiledContract.class);

  /** Ownable */
  private static final Bytes OWNER_SIGNATURE =
      Hash.keccak256(Bytes.of("owner()".getBytes(UTF_8))).slice(0, 4);

  private static final Bytes INITIALIZED_SIGNATURE =
      Hash.keccak256(Bytes.of("initialized()".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes INITIALIZE_OWNER_SIGNATURE =
      Hash.keccak256(Bytes.of("initializeOwner(address)".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes TRANSFER_OWNERSHIP_SIGNATURE =
      Hash.keccak256(Bytes.of("transferOwnership(address)".getBytes(UTF_8))).slice(0, 4);

  /** AddressRegistry */
  private static final Bytes CONTAINS_SIGNATURE =
      Hash.keccak256(Bytes.of("contains(address)".getBytes(UTF_8))).slice(0, 4);

  private static final Bytes DISCOVERY_SIGNATURE =
      Hash.keccak256(Bytes.of("discovery(address)".getBytes(UTF_8))).slice(0, 4);
  ;
  private static final Bytes ADD_TO_REGISTRY_SIGNATURE =
      Hash.keccak256(Bytes.of("addToRegistry(address,address)".getBytes(UTF_8))).slice(0, 4);
  ;
  private static final Bytes REMOVE_FROM_REGISTRY_SIGNATURE =
      Hash.keccak256(Bytes.of("removeFromRegistry(address,address)".getBytes(UTF_8))).slice(0, 4);
  ;

  /** Storage Layout */
  private static final UInt256 INIT = UInt256.ZERO;

  private static final UInt256 OWNER = UInt256.ONE;

  /** Returns */
  private static final Bytes FALSE =
      Bytes.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000000");

  private static final Bytes TRUE =
      Bytes.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000001");

  public AddressRegistryPrecompiledContract(final GasCalculator gasCalculator) {
    super("AddressRegistryPrecompiledContract", gasCalculator);
  }

  private Bytes owner(final MutableAccount contract) {
    return contract.getStorageValue(OWNER);
  }

  private Bytes initialized(final MutableAccount contract) {
    return contract.getStorageValue(INIT);
  }

  private Bytes initializeOwner(final MutableAccount contract, MessageFrame messageFrame) {
    //      if contract.getStorageValue(INIT) == ONE
    //      do nothing.
    //      return FALSE
    //      else
    //      contract.setStorageValue(INIT, UInt256.ONE);
    return TRUE;
  }

  private Bytes transferOwnership(final MutableAccount contract, MessageFrame messageFrame) {
    //      // check msg.sender is owner
    //      if not
    //      do nothing.
    //      return FALSE;
    //      else
    //      contract.setStorageValue(OWNER, neOwner);
    return TRUE;
  }

  private Bytes contains(final MutableAccount contract, MessageFrame messageFrame) {
    //  if not contract.getStorageValue(slot) equal to Uint256.ZERO
    //  return FALSE;
    //  else
    return TRUE;
  }

  private Bytes discovery(final MutableAccount contract, MessageFrame messageFrame) {
    //     return contract.getStorageValue(slot);
  }

  private Bytes addToRegistry(final MutableAccount contract, MessageFrame messageFrame) {
    //     // check msg.sender is owner
    //     if not
    //     do nothing.
    //     return FALSE;
    //     else
    //     contract.setStorageValue(slot, value);
    return TRUE;
  }

  private Bytes removeFromRegistry(final MutableAccount contract, MessageFrame messageFrame) {
    //     // check msg.sender is owner
    //     if not
    //     do nothing.
    //     return FALSE;
    //     else
    //     contract.setStorageValue(slot, Uint256.ZERO);
    return TRUE;
  }

  @Override
  public long gasRequirement(final Bytes input) {
    final Bytes function = input.slice(0, 4);
    if (function.equals(OWNER_SIGNATURE)
        || function.equals(INITIALIZED_SIGNATURE)
        || function.equals(CONTAINS_SIGNATURE)
        || function.equals(DISCOVERY_SIGNATURE)) {
      return 1000;
    } else {
      return 2000;
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
      final WorldUpdater worldUpdater = messageFrame.getWorldUpdater();
      final MutableAccount precompile = worldUpdater.getOrCreate(Address.NATIVE_MINTER);
      if (function.equals(OWNER_SIGNATURE)) {
        return PrecompileContractResult.success(owner(precompile));
      } else if (function.equals(INITIALIZED_SIGNATURE)) {
        return PrecompileContractResult.success(initialized(precompile));
      } else if (function.equals(INITIALIZE_OWNER_SIGNATURE)) {
        return PrecompileContractResult.success(initializeOwner(precompile, input));
      } else if (function.equals(TRANSFER_OWNERSHIP_SIGNATURE)) {
        return PrecompileContractResult.success(transferOwnership(precompile, input));
      } else if (function.equals(CONTAINS_SIGNATURE)) {
        return PrecompileContractResult.success(contains(precompile, input));
      } else if (function.equals(DISCOVERY_SIGNATURE)) {
        return PrecompileContractResult.success(discovery(precompile, input));
      } else if (function.equals(ADD_TO_REGISTRY_SIGNATURE)) {
        return PrecompileContractResult.success(addToRegistry(precompile, input));
      } else if (function.equals(REMOVE_FROM_REGISTRY_SIGNATURE)) {
        return PrecompileContractResult.success(removeFromRegistry(precompile, input));
      } else {
        // @TODO logging the invalid function signature.
        LOG.info("Failed interface not found");
        return PrecompileContractResult.halt(
            null, Optional.of(ExceptionalHaltReason.PRECOMPILE_ERROR));
      }
    }
  }
}
