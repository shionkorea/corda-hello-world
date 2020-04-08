package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.IOUContract;
import com.template.states.IOUState;
import net.corda.core.contracts.Command;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;


// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class IOUFlow extends FlowLogic<Void> {
    private final Integer iouValue;
    private final Party otherParty;

    /**
     * The progress tracker provides checkpoints indicating the progress of the flow to observers.
     */
    private final ProgressTracker progressTracker = new ProgressTracker();

    public IOUFlow(Integer iouValue, Party otherParty) {
        this.iouValue = iouValue;
        this.otherParty = otherParty;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    /**
     * The flow logic is encapsulated within the call() method.
     */
    @Suspendable
    @Override
    public Void call() throws FlowException {
        // We retrieve the notary identity from the network map.
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // We create the transaction components.
        IOUState outputState = new IOUState(iouValue, getOurIdentity(), otherParty);
        List<PublicKey> requiredSigners = Arrays.asList(getOurIdentity().getOwningKey(), otherParty.getOwningKey());
        Command command = new Command<>(new IOUContract.Create(), requiredSigners);

        // We create a transaction builder and add the components.
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(outputState, IOUContract.ID)
                .addCommand(command);

        // Verifying the transaction.
        txBuilder.verify(getServiceHub());

        // Signing the transaction.
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Creating a session with the other party.
        FlowSession otherPartySession = initiateFlow(otherParty);

        // Obtaining the counterparty's signature.
        SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(
                signedTx, Arrays.asList(otherPartySession), CollectSignaturesFlow.tracker()));

        // Finalising the transaction.
        subFlow(new FinalityFlow(fullySignedTx, otherPartySession));

        return null;



//        // We retrieve the notary identity from the network map.
//        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
//
//        // We create the transaction components.
//        IOUState outputState = new IOUState(iouValue, getOurIdentity(), otherParty);
//        Command command = new Command<>(new TemplateContract.Commands.Action(), getOurIdentity().getOwningKey());
//
//        // We create a transaction builder and add the components.
//        TransactionBuilder txBuilder = new TransactionBuilder(notary)
//                .addOutputState(outputState, TemplateContract.ID)
//                .addCommand(command);
//
//        // Signing the transaction.
//        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);
//
//        // Creating a session with the other party.
//        FlowSession otherPartySession = initiateFlow(otherParty);
//
//        // We finalise the transaction and then send it to the counterparty.
//        subFlow(new FinalityFlow(signedTx, otherPartySession));
//
//        return null;
    }
}