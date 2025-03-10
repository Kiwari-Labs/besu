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
  private static final Bytes ALLOWANCE_SIGNATURE = Hash.keccak256(Bytes.of("allowance(address)").getBytes(UTF_8))).slice(0, 4);

  private static final Bytes GRANT_ALLOWANCE_SIGNATURE =
      Hash.keccak256(Bytes.of("grantAllowance(address,address,uint8,uint256,uint32)".getBytes(UTF_8))).slice(0, 4);

  private static final Bytes REVOKE_ALLOWANCE_SIGNATURE =
      Hash.keccak256(Bytes.of("revokeAllowance(uint256)".getBytes(UTF_8))).slice(0, 4);

  private static final Bytes REMAINING_ALLOWANCE_SIGNATURE =
      Hash.keccak256(Bytes.of("remainingAllowance(address)".getBytes(UTF_8))).slice(0, 4);

  private static final Bytes SPENT_ALLOWANCE_SIGNATURE =
      Hash.keccak256(Bytes.of("spentAllowance(address)".getBytes(UTF_8))).slice(0, 4);

  private static final Bytes IS_ALLOWANCE_EXPIRED_SIGNATURE =
      Hash.keccak256(Bytes.of("isAllowanceExpired(address)".getBytes(UTF_8))).slice(0, 4);

  private static final Bytes REMAINING_TIME_BEFORE_PERIOD_RESET_SIGNATURE =
      Hash.keccak256(Bytes.of("remainingTimeBeforePeriodReset(address)".getBytes(UTF_8))).slice(0, 4);

  /** Storage Layout */
  private static final UInt256 INIT_SLOT = UInt256.ZERO;

  private static final UInt256 OWNER_SLOT = UInt256.ONE;

  private static final UInt256 ALLOWANCE_SLOT = UInt256.valueOf(2L);

  /** Struct Allowance */
  // (address, ALLOWANCE_SLOT) 
  // index 0 uint8 allowanceType {0:NONE, 1:BASIC, 2:PERIODIC}
  // index 1 address granter
  // index 2 uint256 feeGrantPerTransaction
  // index 3 uint256 feeGrantPerPeriod
  // index 4 uint256 lastAllowanceResetAt
  // index 5 uint256 expirationAt
  // index 6 uint32 period

  private static final UInt256 SPENT_ALLOWANCE_SLOT = UInt256.valueOf(3L);

  /** Returns */
  private static final Bytes FALSE =
      Bytes.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000000");

  private static final Bytes TRUE =
      Bytes.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000001");

  public GasFeeGrantPrecompiledContract(final GasCalculator gasCalculator) {
    super("GasFeeGrantPrecompiledContract", gasCalculator);
  }

  // for calculate storage slot of mapping(address => Allowance)
  private UInt256 storageSlotAllowance(final Address address) {
    final Bytes slotKey = Bytes.concatenate(ALLOWANCE_SLOT, address);
    return UInt256.fromBytes(Bytes32.leftPad(Hash.keccak256(slotKey)));
  }

  private UInt256 storageSlotSpentAllowance(final Address address) {
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
      if (initialOwner.equals(UInt256.ZERO)) {
        return FALSE;
      }
      contract.setStorageValue(OWNER_SLOT, initialOwner);
      contract.setStorageValue(INIT_SLOT, UInt256.ONE);
      return TRUE;
    }
  }

  private Bytes transferOwnership(
      final MutableAccount contract, final Address senderAddress, final Bytes calldata) {
    if (onlyOwner(contract, senderAddress).equals(FALSE)) {
      return FALSE;
    } else {
      final UInt256 newOwner = UInt256.fromBytes(calldata);
      if (newOwner.equals(UInt256.ZERO)) {
        return FALSE;
      }
      contract.setStorageValue(OWNER_SLOT, newOwner);
      return TRUE;
    }
  }

  private Bytes allowance(final MutableAccount contract, final Address account) {
    // @TODO corp-ais/blockchain-besu
    // return data
  }

  private Bytes revokeAllowance(final MutableAccount contract, final Address senderAddress, final Bytes calldata) {
    if (onlyOwner(contract, senderAddress).equals(FALSE)) {
      return FALSE;
    } else {
      // @TODO corp-ais/blockchain-besu
      return TRUE;
    }
  }

  private Bytes grantAllowance(
      final MutableAccount contract, final Address senderAddress, final Bytes calldata) {
    if (onlyOwner(contract, senderAddress).equals(FALSE)) {
      return FALSE;
    } else {
      // @TODO corp-ais/blockchain-besu
      return TRUE;
    }
  }

  private Bytes remainingAllowance(final MutableAccount contract, final Bytes calldata) {
    final Address address = Address.wrap(calldata.slice(12, 20));
    final UInt256 allowanceSlot = storageSlotAllowance(address); // index 0
    final UInt256 allowanceType = contract.getStorageValue(allowanceSlot);
    if (allowanceType.equals(UInt256.ONE)) {
      return UInt256.MAX_VALUE;
    } else if (allowanceType.equals(UInt256.TWO)) {
      return 
    } else {
      return UInt256.ZERO;
    }
  }

  private Bytes spentAllowance(final MutableAccount contract, final Bytes calldata) {
    final Address address = Address.wrap(calldata.slice(12, 20));
    final UInt256 allowanceSlot = storageSlotAllowance(address); // index 0
    final UInt256 allowanceType = contract.getStorageValue(allowanceSlot);
    if (allowanceType.equals(UInt256.ONE)) {
      return contract.getStorageValue(storageSlotSpentAllowance(address));
    } else if (allowanceType.equals(UInt256.TWO)) {
      // blockNumber
      // if (blockNumber <= expirationAt) {
      //   if (blockNumber - lastAllowanceResetAt >= period) {
      //      return UInt256.ZERO;
      //   }
      // }
      return contract.getStorageValue(storageSlotSpentAllowance(address));
    } else {
      return UInt256.ZERO;
    }
  }

  private Bytes isAllowanceExpired(final MutableAccount contract, final Bytes calldata) {
    final Address address = Address.wrap(calldata.slice(12, 20));
    final UInt256 allowanceSlot = storageSlotAllowance(address); // index 0
    // return blockNumber >= (storageSlotAllowance(address).add(4L));
  }

  private Bytes remainingTimeBeforePeriodReset(final MutableAccount contract, final Bytes calldata) {
    // @TODO corp-ais/blockchain-besu
  }

  @Override
  public long gasRequirement(final Bytes input) {
    final Bytes function = input.slice(0, 4);
    if (function.equals(OWNER_SIGNATURE)
        || function.equals(INITIALIZED_SIGNATURE)
        || function.equals(STATUS_SIGNATURE)) {
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
      } else if (function.equals(ALLOWANCE_SIGNATURE)) {
        return PrecompileContractResult.success(allowance(precompile));
      } else if (function.equals(GRANT_ALLOWANCE_SIGNATURE)) {
        return PrecompileContractResult.success(grantAllowance(precompile));
      } else if (function.equals(REVOKE_ALLOWANCE_SIGNATURE)) {
        return PrecompileContractResult.success(revokeAllowance(precompile, senderAddress, calldata));
      } else {
        LOG.debug("Failed function {} not found", function);
        return PrecompileContractResult.halt(
            null, Optional.of(ExceptionalHaltReason.PRECOMPILE_ERROR));
      }
    }
  }
}
