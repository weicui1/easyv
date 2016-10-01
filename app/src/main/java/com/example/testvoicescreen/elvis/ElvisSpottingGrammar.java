/*
 * Copyright (C) 2013 Motorola Mobility LLC.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 *
 */

package com.example.testvoicescreen.elvis;

import java.util.ArrayList;
import java.util.List;

import com.nuance.dragon.toolkit.elvis.Constraint;
import com.nuance.dragon.toolkit.elvis.Grammar;
import com.nuance.dragon.toolkit.elvis.WordSlot;
import com.nuance.dragon.toolkit.grammar.Word;

public class ElvisSpottingGrammar {

    public static Grammar getGrammar(List<String> spotCommands) {
        List<WordSlot> wordSlots = new ArrayList<WordSlot>();
        List<Constraint> constraints = new ArrayList<Constraint>();

        if (spotCommands != null && spotCommands.size() > 0) {
            WordSlot localCmd = new WordSlot("spotCommands");
            for (String command : spotCommands) {
                localCmd.addWord(new Word(command));
            }
            wordSlots.add(localCmd);
        }

        /* create wordSlot and values for [garbage] */
        WordSlot garbage = new WordSlot("garbage", WordSlot.Types.WORDSLOT_GENERIC_SHORT);
        wordSlots.add(garbage);

        /* Create Constraint for the local command. */
        Constraint localcmdConst = new Constraint("spotCommand");
        localcmdConst.addTransition(Constraint.START, "spotCommands", 0);
        localcmdConst.addTransition("spotCommands", Constraint.END, 0);
        constraints.add(localcmdConst);

        /* Unidentified commands */
        Constraint garbageConstraint = new Constraint("garbageConst");
        garbageConstraint.addTransition(Constraint.START, "garbage", 0);
        garbageConstraint.addTransition("garbage", Constraint.END, 0);
        constraints.add(garbageConstraint);

        return Grammar.createGrammar(wordSlots, constraints);
    }
}
