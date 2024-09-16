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

public class StatefulPrecompiledContract extends AbstractPrecompiledContract {
    
    private static final Logger LOG = LoggerFactory.getLogger(StatefulPrecompiledContract.class);

    // temporary use `0x0000000000000000000000000000000000000802` same as in moonbeam network.
    private static final Address IERC20_ADDRESS = Address.fromHexString("0x0000000000000000000000000000000000000802");

    private static final Bytes APPROVE_SIGNATURE = Hash.keccak256(Bytes.of("approve(address,uint256)".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes ALLOWANCE_SIGNATURE = Hash.keccak256(Bytes.of("allowance(address,address)".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes BALANCEOF_SIGNATURE = Hash.keccak256(Bytes.of("balanceOf(address)".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes DECIMALS_SIGNATURE = Hash.keccak256(Bytes.of("decimals()".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes NAME_SIGNATURE = Hash.keccak256(Bytes.of("name()".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes SYMBOL_SIGNATURE = Hash.keccak256(Bytes.of("symbol()".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes TRANSFER_SIGNATURE = Hash.keccak256(Bytes.of("transfer(address,uint256)".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes TRANSFERFROM_SIGNATURE = Hash.keccak256(Bytes.of("transferFrom(address,address,uint256)".getBytes(UTF_8))).slice(0, 4);
    private static final Bytes TOTALSUPPLY_SIGNATURE = Hash.keccak256(Bytes.of("totalSupply()".getBytes(UTF_8))).slice(0, 4);

    // Transfer(address, address, uint256)
    // Approval(address, address, uint256)

    // Retrieving ERC20 data directly from the storage slot will return 0x0 or empty 
    // because the data is hardcoded and not stored in the database.
    // Only the approval data can be retrieved from the storage slot.

    public StatefulPrecompiledContract(final GasCalculator gasCalculator) {
        super("StatefulPrecompiledContract", gasCalculator);
    }

    @Override
    public long gasRequirement(final Bytes input) {
        final Bytes function = input.slice(0, 4);
        if (function.equals(APPROVE_SIGNATURE) ||
            function.equals(TRANSFER_SIGNATURE) ||
            function.equals(TRANSFERFROM_SIGNATURE)) {
            // gas used for write to database
            return 3000;
        } else {
            // gas used for read from database
            return 1000;
        }
    }

    private Bytes decimals() {
        return Bytes.ofUnsignedInt(18);
    }

    private Bytes name() {
        return Bytes.of("Ethereum".getBytes(UTF_8));
    }

    private Bytes symbol() {
        return Bytes.of("ETH".getBytes(UTF_8));
    }

    private Bytes totalSupply() {
        return Bytes.ofUnsignedInt(0);
    }

    private Bytes balanceOf(final MutableAccount address) {
        return address.getBalance();
    }

    // private updateBalance(final MutableAccount from, final MutableAccount to, MessageFrame messageFrame) {
    //     from.decreaseBalance(value);
    //     to.incrementBalance(value);
    //     emit event
    // }

    // private updateApprove(final MutableAccount contract, final MutableAccount owner, final MutableAccount spender, Wei value, bool emitEvent, MessageFrame messageFrame) {
    //     if (emitEvent) {
    //        emit event 
    //     }
    // }     

    // private Bytes approve(final MutableAccount contract, final Bytes payload, MessageFrame messageFrame) {
    //     updateApprove(contract, owner, spender, value, true, messageFrame)
    //     return TRUE
    // }

    // private Bytes spendAllowance(final MutableAccount contract, final Bytes payload, MessageFrame messageFrame) {
    //     updateApprove(contract, owner, spender, value, false, messageFrame)
    // }

    // private Bytes transfer(final Bytes payload, MessageFrame messageFrame) {
    //     updateBalance(from, to, value);
    //     return TRUE
    // }      

    // private Bytes transferFrom(final MutableAccount contract, final Bytes payload, MessageFrame messageFrame ) {
    //     spendAllowance(contract, from, messageFrame.sender(), value);
    //     updateBalance(from, to, value);
    //     return TRUE
    // }

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
            final MutableAccount erc20 = worldUpdater.getOrCreate(IERC20_ADDRESS);
            if (function.equals(APPROVE_SIGNATURE)) {
                return PrecompileContractResult.success(
                    approve(erc20, input, messageFrame)
                    );
            } 
            else if (function.equals(ALLOWANCE_SIGNATURE)) {
                return PrecompileContractResult.success(
                    allowance(input)
                    );
            }
            else if (function.equals(BALANCEOF_SIGNATURE)) {
                return PrecompileContractResult.success(
                    balanceOf(input)
                    );
            }
            else if (function.equals(DECIMALS_SIGNATURE)) {
                return PrecompileContractResult.success(
                    decimals()
                    );
            }
            else if (function.equals(NAME_SIGNATURE)) {
                return PrecompileContractResult.success(
                    name()
                    );
            }
            else if (function.equals(TRANSFER_SIGNATURE)) {
                return PrecompileContractResult.success(
                    transfer(input, messageFrame));
            }
            else if (function.equals(TRANSFERFROM_SIGNATURE)) {
                return PrecompileContractResult.success(
                    transferFrom(erc20, input, messageFrame)
                    );
            }
            else if (function.equals(TOTALSUPPLY_SIGNATURE)) {
                return PrecompileContractResult.success(
                    totalSupply()
                    );
            } else {
                LOG.info("Failed interface not found");
                return PrecompileContractResult.halt(null, Optional.of(ExceptionalHaltReason.PRECOMPILE_ERROR));
            }
        }
    }
}
