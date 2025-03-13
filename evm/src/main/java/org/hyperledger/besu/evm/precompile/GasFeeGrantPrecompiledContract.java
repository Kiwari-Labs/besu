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
import org.apache.tuweni.units.bigints.UInt256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GasFeeGrantPrecompiledContract extends AbstractPrecompiledContract {
  private static final Logger LOG = LoggerFactory.getLogger(GasFeeGrantPrecompiledContract.class);

  /** Ownable */
  private static final Bytes OWNER_SIGNATURE =
      Hash.keccak256(Bytes.of("owner()".getBytes(UTF_8))).slice(0, 4);

  private static final Bytes INITIALIZED_SIGNATURE =
      Hash.keccak256(Bytes.of("initialized()".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes INITIALIZE_OWNER_SIGNATURE =
      Hash.keccak256(Bytes.of("initializeOwner(address)".getBytes(UTF_8))).slice(0, 4);
  private static final Bytes TRANSFER_OWNERSHIP_SIGNATURE =
      Hash.keccak256(Bytes.of("transferOwnership(address)".getBytes(UTF_8))).slice(0, 4);

  /** GasFeeGrant */
  private static final Bytes SET_FEE_GRANT_SIGNATURE =
      Hash.keccak256(Bytes.of("setFeeGrant(address,address,address,uint256,uint32,uint256,uint256)".getBytes(UTF_8))).slice(0, 4);

  private static final Bytes REVOKE_FEE_GRANT_SIGNATURE =
      Hash.keccak256(Bytes.of("revokeFeeGrant(address,address)".getBytes(UTF_8))).slice(0, 4);

  private static final Bytes PERIOD_CAN_SPEND_SIGNATURE =
      Hash.keccak256(Bytes.of("periodCanSpend(address,address)".getBytes(UTF_8))).slice(0, 4);

  private static final Bytes PERIOD_RESET_SIGNATURE =
      Hash.keccak256(Bytes.of("periodReset(address,address)".getBytes(UTF_8))).slice(0, 4);

  private static final Bytes EXPIRED_SIGNATURE =
      Hash.keccak256(Bytes.of("expired(address,address)".getBytes(UTF_8))).slice(0, 4);

  private static final Bytes GRANT_SIGNATURE =
      Hash.keccak256(Bytes.of("grant(address,address)".getBytes(UTF_8))).slice(0, 4);

  /** Storage Layout */
  private static final UInt256 INIT_SLOT = UInt256.ZERO;

  private static final UInt256 OWNER_SLOT = UInt256.ONE;

  private static final UInt256 GRANTS_SLOT = UInt256.valueOf(2L);

  /** Struct Grant */
  // index 0 address granter
  // index 1 uint8 allowance {0:NONE, 1:BASIC, 2:PERIODIC}
  // index 2 uint256 spendLimit
  // index 3 uint256 periodLimit
  // index 4 uint256 expiration
  // index 5 uint32 period

  private static final UInt256 PERIOD_RESETS_SLOT = UInt256.valueOf(3L);

  private static final UInt256 SPEND_PERIODS_SLOT = UInt256.valueOf(4L);

  /** Returns */
  private static final Bytes FALSE =
      Bytes.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000000");

  private static final Bytes TRUE =
      Bytes.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000001");

  public GasFeeGrantPrecompiledContract(final GasCalculator gasCalculator) {
    super("GasFeeGrantPrecompiledContract", gasCalculator);
  }

  private UInt256 storageSlotGrant(final Address grantee, final Address program) {
    final Bytes slotKey = Bytes.concatenate(GRANTS_SLOT, grantee);
    return UInt256.fromBytes(Bytes32.leftPad(Hash.keccak256(slotKey)));
  }

  private UInt256 storageSlotPeriodResets(final Address grantee, final Address program) {
    final Bytes slotKey = Bytes.concatenate(SPENT_ALLOWANCE_SLOT, address);
    return UInt256.fromBytes(Bytes32.leftPad(Hash.keccak256(slotKey)));
  }

  private UInt256 storageSlotSpendPeriods(final Address grantee, final Address program) {
    final Bytes slotKey = Bytes.concatenate(SPENT_ALLOWANCE_SLOT, address);
    return UInt256.fromBytes(Bytes32.leftPad(Hash.keccak256(slotKey)));
  }

  /** Modifier */
  private Bytes onlyOwner(final MutableAccount contract, final Address senderAddress) {
    final Address storedOwner = Address.wrap(contract.getStorageValue(OWNER_SLOT).slice(12, 20));
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

  private Bytes initializeOwner(final MutableAccount contract, final Bytes calldata) {
    if (initialized(contract).equals(TRUE)) {
      return FALSE;
    } else {
      final UInt256 initialOwner = UInt256.fromBytes(calldata);
      if (initialOwner.isZero()) {
        return FALSE;
      }
      contract.setStorageValue(OWNER_SLOT, initialOwner);
      contract.setStorageValue(INIT_SLOT, UInt256.ONE);
      return TRUE;
    }
  }

  private Bytes transferOwnership(
      final MutableAccount contract, final Address senderAddress, final Bytes calldata) {
    if (onlyOwner(contract, senderAddress).isZero()) {
      return FALSE;
    } else {
      final UInt256 newOwner = UInt256.fromBytes(calldata);
      if (newOwner.isZero()) {
        return FALSE;
      }
      contract.setStorageValue(OWNER_SLOT, newOwner);
      return TRUE;
    }
  }

  // @TODO corp-ai/blockchain-besu
  // isGranted()

  private Bytes grant(final MutableAccount contract, final Address account) {
    // @TODO corp-ais/blockchain-besu
    // return data
  }

  private Bytes setFeeGrant(final MutableAccount contract, final Address senderAddress, final Bytes calldata) {
    if (onlyOwner(contract, senderAddress).isZero()) {
      return FALSE;
    } else {
      // @TODO corp-ais/blockchain-besu
      return TRUE;
    }
  }

  private Bytes revokeFeeGrant(
      final MutableAccount contract, final Address senderAddress, final Bytes calldata) {
    if (onlyOwner(contract, senderAddress).isZero()) {
      return FALSE;
    } else {
      // @TODO corp-ais/blockchain-besu
      return TRUE;
    }
  }

  private Bytes periodCanSpend(final MutableAccount contract, final Bytes calldata) {
    final Address grantee = Address.wrap(calldata.slice(12, 20));
    // final Address program = Address.wrap(calldata.slice(,));
    final UInt256 allowanceForAll = contract.getStorageValue(storageSlotAllowance(grantee, Address.ZERO));
    final UInt256 allowance = contract.getStorageValue(storageSlotAllowance(grantee, program));
    if (allowanceForAll.equals(UInt256.TWO) || allowance.equals(UInt256.TWO)) {
      return UInt256.MAX_VALUE;
    } else {
      return FALSE;
    }
  }

  private Bytes periodReset(final MutableAccount contract, final Bytes calldata) {
    final Address address = Address.wrap(calldata.slice(12, 20));
    final UInt256 allowance = contract.getStorageValue(storageSlotAllowance(address));
    if (allowance.equals(UInt256.ONE)) {
      return contract.getStorageValue(storageSlotSpentAllowance(address));
    } else if (allowance.equals(UInt256.TWO)) {
      return contract.getStorageValue(storageSlotSpentAllowance(address));
    } else {
      return FALSE;
    }
  }

  private Bytes expired(final MutableAccount contract, final Bytes calldata) {
    final Address address = Address.wrap(calldata.slice(12, 20));
    final UInt256 expiration = contract.getStorageValue(storageSlotGrant(address).add(4L));
    if (isGranted(address)) {
      if (expiration.isZero()) {
        return FALSE;
      } else {
        // return block.number >= expiration;
        return TRUE;
      }
    } else {
      return TRUE;
    }
  }

  @Override
  public long gasRequirement(final Bytes input) {
    final Bytes function = input.slice(0, 4);
    if (function.equals(OWNER_SIGNATURE)
        || function.equals(INITIALIZED_SIGNATURE)
        || function.equals(STATUS_SIGNATURE)
        || function.equals(GRANT_SIGNATURE)
        || function.equals(PERIOD_CAN_SPEND_SIGNATURE)
        || function.equals(PERIOD_RESET_SIGNATURE)
        || function.equals(EXPIRED_SIGNATURE)) {
      // gas cost for read operation.
      return 1000;
    } else {
      // gas cost for write operation.
      return 2000;
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
      final Bytes function = input.slice(0, 4);
      final Bytes calldata = input.slice(4);
      final WorldUpdater worldUpdater = messageFrame.getWorldUpdater();
      final Address senderAddress = messageFrame.getSenderAddress();
      final MutableAccount precompile = worldUpdater.getOrCreate(Address.GASFEE_GRANT);
      if (function.equals(OWNER_SIGNATURE)) {
        return PrecompileContractResult.success(owner(precompile));
      } else if (function.equals(INITIALIZED_SIGNATURE)) {
        return PrecompileContractResult.success(initialized(precompile));
      } else if (function.equals(INITIALIZE_OWNER_SIGNATURE)) {
        return PrecompileContractResult.success(initializeOwner(precompile, calldata));
      } else if (function.equals(TRANSFER_OWNERSHIP_SIGNATURE)) {
        return PrecompileContractResult.success(
            transferOwnership(precompile, senderAddress, calldata));
      } else if (function.equals(GRANT_SIGNATURE)) {
        return PrecompileContractResult.success(grant(precompile));
      } else if (function.equals(SET_FEE_GRANT_SIGNATURE)) {
        return PrecompileContractResult.success(setFeeGrant(precompile));
      } else if (function.equals(REVOKE_FEE_GRANT_SIGNATURE)) {
        return PrecompileContractResult.success(revokeFeeGrant(precompile, senderAddress, calldata));
      } else {
        LOG.debug("Failed function {} not found", function);
        return PrecompileContractResult.halt(
            null, Optional.of(ExceptionalHaltReason.PRECOMPILE_ERROR));
      }
    }
  }
}
