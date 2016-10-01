/*
 * Copyright (C) 2013 Motorola Mobility LLC.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 *
 */

package com.example.testvoicescreen;

/**
 * Interface to control recognition state machines.
 */
public interface IRecogController {

    /**
     * This method starts the recognition state machine.
     */
    public void startRecog();

    /**
     * This method stops and cleans up the recognition state machine.
     */
    public void cleanUp();
};
