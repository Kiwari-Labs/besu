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
import org.apache.tuweni.units.bigints.UInt8;
import org.apache.tuweni.units.bigints.UInt256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatefulSortedCircularLinkedListPrecompiledContract extends AbstractPrecompiledContract {
    
    private static final Logger LOG = LoggerFactory.getLogger(StatefulSortedCircularLinkedListPrecompiledContract.class);
    
    // TODO Define MAX_SIZE for safety, 
    // we should not allowing iterate the list and take too long.
    // case 100ms 1/10 of 1 sec block time.
    // case 250ms 1/4 of 1 sec block time. based on blocktime of arbitrum and unichain
    // case 750ms 3/4 of 1 sec block time. 
    // case 1000ms 1 sec block time. 

    /** CONSTANT VARIABLE */
    private long static final MAX_SIZE = 5_000_000; // assume optimal number.
    private Uint8 static final ONE_BIT = Uint8.valueOf(1);
    private Uint8 static final SENTINEL = Uint8.valueOf(0);
    private Uint8 static final NEXT = Uint8.valueOf(1);
    private Uint8 static final PREVIOUS = Uint8.valueOf(0);
    private Bytes static final TRUE = UInt256.valueOf(0); // `one` can be decode to `true` in solidity type bool
    private Bytes static final FALSE = UInt256.valueOf(0); // `zero` can be decode to `false` in solidity type bool

    /** CALL FUNCTION SIGNATURE */
    private static final Bytes REMOVE_SIGNATURE = Hash.keccak256(Bytes.of("remove(uint256)".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes INSERT_SIGNATURE = Hash.keccak256(Bytes.of("insert(uint256,uint256)".getBytes(UTF_8))).slice(0, 4);

    /** STATIC CALL FUNCTION SIGNATURE */
    private static final Bytes FIND_SIGNATURE = Hash.keccak256(Bytes.of("find(uint256)".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes FRONT_SIGNATURE = Hash.keccak256(Bytes.of("front()".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes BACK_SIGNATURE = Hash.keccak256(Bytes.of("back()".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes LIST_SIGNATURE = Hash.keccak256(Bytes.of("list()".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes REVERSE_LIST_SIGNATURE = Hash.keccak256(Bytes.of("rlist()".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes MIDDLE_SIGNATURE = Hash.keccak256(Bytes.of("middle()".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes NEXT_SIGNATURE = Hash.keccak256(Bytes.of("next(uint256)".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes PREVIOUS_SIGNATURE = Hash.keccak256(Bytes.of("previous(uint256)".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes SIZE_SIGNATURE = Hash.keccak256(Bytes.of("size()".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes MAX_SIZE_SIGNATURE = Hash.keccak256(Bytes.of("max_size()".getBytes(UTF_8))).slice(0, 4);

    public StatefulSortedCircularLinkedListPrecompiledContract(final GasCalculator gasCalculator) {
        super("StatefulSortedCircularLinkedListPrecompiledContract", gasCalculator);
    }

    // TODO reuse blake2bf for storage slot.

    /** CalculateStorageSLot for List size */
    private Uint256 CalculateStorageSlot(final Address address, final Bytes pointer, final Uint256 element) {
        // Hash.keccak256(address, pointer, element); // covert into Uint256
        return Uint256.valueOf(1);
    }
    
    /** CalculateStorageSLot for Node */
    private Uint256 CalculateStorageSlot(final Address address, final Bytes pointer, final Uint256 element, final Uint8 direction) {
        // Hash.keccak256(address, pointer, element, direction); // convert into Uint256
        return Uint256.valueOf(1);
    }

    /** STATIC CALL FUNCTION */
    private Bytes find(final MutableAccount address, final Bytes payload) {
        // final Uint256 pointer = payload; // storage pointer
        // final Uint256 element = payload; //  given element
        // final Uint256 previousElement = calculateStorageSlot(messageFrame.sender, pointer, element, PREVIOUS);
        // final Uint256 nextElement = calculateStorageSlot(messageFrame.sender, pointer, SENTINEL, NEXT);
        // final Bytes output = (nextElement == element) || (previousElement > 0 );
        // return output;
        return address.getStorageValue(slot);
    }

    private Bytes front(final MutableAccount address) {
        // final Uint256 pointer = payload; // storage pointer
        // final Uint256 slot = calculateStorageSlot(messageFrame.sender, pointer, SENTINEL, NEXT);
        return address.getStorageValue(slot);
    }

    private Bytes middle(final MutableAccount address) {
        // final Uint256 slot = calculateStorageSlot(messageFrame.sender, pointer, Uint256.valueOf(0)); // get size first
        // 
        // perform search traversal.
        return PrecompileContractResult.success(address.getStorageValue(slot));
    }

    private Bytes next(final MutableAccount address, final Bytes payload) {
        // final Uint256 pointer = payload; // storage pointer
        // final Uint256 element = payload; //  given element
        // final Uint256 slot = calculateStorageSlot(messageFrame.address, pointer, element, NEXT);
        return address.getStorageValue(slot);
    }

    private Bytes previous(final MutableAccount address, final Bytes payload) {
        // final Uint256 pointer = payload; // storage pointer
        // final Uint256 element = payload; //  given element
        // final Uint256 slot = calculateStorageSlot(messageFrame.address, pointer, element, PERVIOUS);
        return address.getStorageValue(slot);
    }

    private Bytes size(final MutableAccount address) {
        // final Uint256 pointer = payload; // storage pointer
        // final Uint256 slot = calculateStorageSlot(messageFrame.address, pointer, Uint256.valueOf(0));
        return address.getStorageValue(slot);
    }

    private Bytes maxSize() {
        return Uint256.valueOf(MAX_SIZE);
    }

    private Bytes back(final MutableAccount address) {
        // final Uint256 pointer = payload; // storage pointer
        // final Uint256 slot = calculateStorageSlot(address, pointer, SENTINEL, PREVIOUS);
        return address.getStorageValue(slot);
    }

    private Bytes list(final MutableAccount address) {
        // final Uint256 pointer = payload; // storage pointer
        // final Uint256 slot = calculateStorageSlot(address, pointer, SENTINEL, PREVIOUS);
        return address.getStorageValue(slot);
    }

    private Bytes reverseList(final MutableAccount address) {
        // final Uint256 pointer = payload; // storage pointer
        // final Uint256 slot = calculateStorageSlot(address, pointer, SENTINEL, PREVIOUS);
        return address.getStorageValue(slot);
    }

    /** CALL FUNCTION */
    private Bytes insert(final MutableAccount address, final Bytes payload) {
        final boolean exist = contain(address, payload, messageFrame);
        final Uint256 s = size(address, payload, messageFrame);
        if exist {
            // do nothing
            return FALSE;
        } else if (s.add(1) > MAX_SIZE) {
            // do nothing
            return FALSE;
        } else {
            final Uint256 front = front(address, payload, messageFrame);
            final Uint256 back = back(address, payload, messageFrame);
            if (s == SENTINEL) {
                // insert first node
                final Uint256 previousSentinel = calculateStorageSlot(address, pointer, SENTINEL, PREVIOUS);
                final Uint256 nextSentinel = calculateStorageSlot(address, pointer, SENTINEL, NEXT);
                final Uint256 previousElement = calculateStorageSlot(address, pointer, element, PREVIOUS);
                final Uint256 nextElement = calculateStorageSlot(address, pointer, element, NEXT);
                address.setStorageValue(previousSentinel, element);
                address.setStorageVaule(nextSentinel, element);
                address.setStorageValue(previousElement, SENTINEL);
                address.setStorageVaule(nextElement, SENTINEL);
            } else if (element < front) {
                // push_front
                final Uint256 nextSentinel = calculateStorageSlot(address, pointer, SENTINEL, NEXT);
                final Uint256 front = calculateStorageSlot(address, pointer, SENTINEL, NEXT);
                final Uint256 previousElement = calculateStorageSlot(address, pointer, element, PREVIOUS);
                final Uint256 nextElement = calculateStorageSlot(address, pointer, element, PREVIOUS);
                address.setStorageValue(nextSentinel, element);
                address.setStorageVaule(front, element);
                address.setStorageValue(previousElement, SENTINEL);
                address.setStorageVaule(nextElement, front);
            } else if (element > back) {
                // push_back
                final Uint256 previousSentinel = calculateStorageSlot(address, pointer, SENTINEL, NEXT);
                final Uint256 back = calculateStorageSlot(address, pointer, SENTINEL, NEXT);
                final Uint256 previousElement = calculateStorageSlot(address, pointer, element, PREVIOUS);
                final Uint256 nextElement = calculateStorageSlot(address, pointer, element, PREVIOUS);
                address.setStorageValue(previousSentinel, element);
                address.setStorageVaule(back, element);
                address.setStorageValue(previousElement, back);
                address.setStorageVaule(nextElement, SENTINEL);
            } else {
                // insert specific element
                Uint256 tmpCurr;
                if (element - front <= back - element) {
                    tmpCurr = front;
                    while (element > tmpCurr) {
                        tmpCurr = next(address, tmpCurr, messageFrame);
                    }
                } else {
                    tmpCurr = back;
                    while (element < tmpCurr) {
                        tmpCurr = previous(address, tmpCurr, massageFrame);
                    }
                }
                Uint256 tmpPrev = calculateStorageSlot(address, pointer, tmpCurr, NEXT);
                address.setStorageValue(tmpPrev, element);
                address.setStorageVaule(tmpCurr, element);
                address.setStorageValue(previousElement, tmpPrev);
                address.setStorageVaule(nextElement, tmpCurr);
            }
            return TRUE;
        }
    }

    private Bytes remove(final MutableAccount address, final Bytes payload, @Nonnull final MessageFrame messageFrame) {
        final boolean exist = contain(address, payload, messageFrame);
        if exist {
            // pop_front

            // pop_back

            // remove specific element
            return TRUE;
        } else {
            // do nothing
            return FALSE;
        }
    }

    @Override
    public long gasRequirement(final Bytes input) {
        final Bytes function = input.slice(0, 4);
        if (function.equals(REMOVE_SIGNATURE) ||
            function.equals(INSERT_SIGNATURE) {
            // for call function should be high
            // TODO calculate from time and space complexity of algorithms
            return 5000;
        } else {
            // for static call function should be low
            return 2500;
        }
    }

    @Nonnull
    @Override
    public PrecompileContractResult computePrecompile(final Bytes input, @Nonnull final MessageFrame messageFrame) {
        if (input.isEmpty()) {
            return PrecompileContractResult.halt(null, Optional.of(ExceptionalHaltReason.PRECOMPILE_ERROR));
        } else {
            // function signature selector.
            final Bytes function = input.slice(0, 4);
            final WorldUpdater worldUpdater = messageFrame.getWorldUpdater();
            final MutableAccount sender = worldUpdater.getOrCreate(messageFrame..getSenderAddress());
            if (function.equals(FIND_SIGNATURE)) {
                return PrecompileContractResult.success(
                    find(sender, payload, messageFrame)
                    );
            } else if (function.equals(FRONT_SIGNATURE)) {
                return PrecompileContractResult.success(
                    front(sender, payload, messageFrame)
                    );
            } else if (function.equals(LIST_SIGNATURE)) {
                return PrecompileContractResult.success(
                    list(sender, payload, messageFrame)
                    );
            } else if (function.equals(LIST_SIGNATURE)) {
                return PrecompileContractResult.success(
                    reverseList(sender, payload, messageFrame)
                    );
            } else if (function.equals(MIDDLE_SIGNATURE)) {
                return PrecompileContractResult.success(
                    middle(sender, payload, messageFrame)
                    );
            } else if (function.equals(NEXT_SIGNATURE)) {
                return PrecompileContractResult.success(
                    next(sender, payload, messageFrame)
                    );
            } else if (function.equals(PREVIOUS_SIGNATURE)) {
                return PrecompileContractResult.success(
                    previous(sender, payload, messageFrame));
            } else if (function.equals(SIZE_SIGNATURE)) {
                return PrecompileContractResult.success(
                    size(sender, payload, messageFrame)
                    );
            } else if (function.equals(MAX_SIZE_SIGNATURE)) {
                return PrecompileContractResult.success(maxSize());
            } else if (function.equals(BACK_SIGNATURE)) {
                return PrecompileContractResult.success(
                    back(sender, payload, messageFrame)
                    );
            } else if (function.equals(REMOVE_SIGNATURE)) {
                return PrecompileContractResult.success(
                    remove(sender, payload, messageFrame)
                    );
            } else if (function.equals(INSERT_SIGNATURE)) {
                return PrecompileContractResult.success(
                    insert(sender, payload, messageFrame)
                    );
            } else {
                LOG.trace("Failed interface id invalid");
                return PrecompileContractResult.halt(null, Optional.of(ExceptionalHaltReason.PRECOMPILE_ERROR));
            }
        }
    }
}
