/*
Copyright (c) 2009-2014, Andrew M. Martin
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
conditions are met:

 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following
   disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
   disclaimer in the documentation and/or other materials provided with the distribution.
 * Neither the name of Pandam nor the names of its contributors may be used to endorse or promote products derived from this
   software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/
package org.pandcorps.pandax.touch;

import org.pandcorps.core.*;
import org.pandcorps.pandam.*;
import org.pandcorps.pandam.Panput.*;
import org.pandcorps.pandam.event.action.*;

public class TouchTabs {
    private final int x;
    private final int y;
    private final int z;
    private final int buttonWidth;
    private final int numButtonsDisplayed;
    private final TouchButton leftButton;
    private final TouchButton rightButton;
    private final TouchButton[] buttons;
    private int currentFirstButton = 0;
    
    public TouchTabs(final int z, final Panmage left, final Panmage leftAct, final Panmage right, final Panmage rightAct, final TouchButton... buttons) {
        final Pangine engine = Pangine.getEngine();
        final Panple buttonSize = left.getSize();
        this.y = engine.getEffectiveHeight() - (int) buttonSize.getY();
        this.z = z;
        buttonWidth = (int) buttonSize.getX();
        this.buttons = buttons;
        final int screenWidth = engine.getEffectiveWidth();
        final int max = screenWidth / buttonWidth, total = buttons.length;
        final int totalDisplayed = max > total ? max : total;
        final int totalWidth = totalDisplayed * buttonWidth;
        x = (screenWidth - totalWidth) / 2;
        if (max > total) {
            numButtonsDisplayed = max - 2;
            final Panlayer layer = buttons[0].getActor().getLayer();
            final String id = Pantil.vmid();
            leftButton = newButton(layer, "left." + id, x, y, z, left, leftAct, true, new Runnable() {
                @Override public final void run() {
                    left(); }});
            rightButton = newButton(layer, "right." + id, x + (max - 1) * buttonWidth, y, z, right, rightAct, true, new Runnable() {
                @Override public final void run() {
                    right(); }});
        } else {
            numButtonsDisplayed = total;
            leftButton = null;
            rightButton = null;
        }
        initButtons();
    }
    
    public final static TouchButton newButton(final Panlayer layer, final String name, final Panmage img, final Panmage imgAct, final Runnable listener) {
        return newButton(layer, name, 0, 0, 0, img, imgAct, false, listener);
    }
    
    private final static TouchButton newButton(final Panlayer layer, final String name, final int x, final int y, final float z,
                                               final Panmage img, final Panmage imgAct, final boolean active, final Runnable listener) {
        final Pangine engine = Pangine.getEngine();
        final TouchButton button = new TouchButton(engine.getInteraction(), layer, name, x, y, z, img, imgAct, true);
        final Panctor actor = button.getActor();
        if (active) {
            engine.registerTouchButton(button);
        } else {
            actor.detach();
        }
        actor.register(button, Actions.newEndListener(listener));
        return button;
    }
    
    private final void initButtons() {
        final Pangine engine = Pangine.getEngine();
        int x = this.x + ((leftButton == null) ? 0 : buttonWidth);
        final int size = buttons.length;
        for (int i = 0; i < size; i++) {
            final int buttonIndex = (currentFirstButton + i) % size;
            final TouchButton button = buttons[buttonIndex];
            if (i >= numButtonsDisplayed) {
                button.detach();
                continue;
            }
            if (!engine.isTouchButtonRegistered(button)) {
                button.reattach();
            }
            button.setPosition(x, y);
            x += buttonWidth;
            final Panctor actor = button.getActor();
            if (actor != null) {
                actor.getPosition().setZ(z);
            }
        }
    }
    
    private final void left() {
        currentFirstButton--;
        if (currentFirstButton < 0) {
            currentFirstButton = buttons.length - 1;
        }
        initButtons();
    }
    
    private final void right() {
        currentFirstButton++;
        if (currentFirstButton >= buttons.length) {
            currentFirstButton = 0;
        }
        initButtons();
    }
    
    public final void destroy() {
        TouchButton.destroy(leftButton);
        TouchButton.destroy(rightButton);
        for (final TouchButton button : buttons) {
            TouchButton.destroy(button);
        }
    }
}