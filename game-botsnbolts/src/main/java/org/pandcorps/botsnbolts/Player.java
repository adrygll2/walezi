/*
Copyright (c) 2009-2016, Andrew M. Martin
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
package org.pandcorps.botsnbolts;

import org.pandcorps.game.actor.*;
import org.pandcorps.pandam.*;
import org.pandcorps.pandam.event.action.*;
import org.pandcorps.pandax.in.*;
import org.pandcorps.pandax.tile.*;

public final class Player extends GuyPlatform {
    protected final static int PLAYER_X = 6;
    protected final static int PLAYER_H = 23;
    private final static int SHOOT_DELAY_DEFAULT = 10;
    private final static int SHOOT_DELAY_RAPID = 3;
    private final static int SHOOT_DELAY_SPREAD = 15;
    private final static int SHOOT_TIME = 12;
    private final static int INVINCIBLE_TIME = 60;
    private final static int HURT_TIME = 20;
    private final static int RUN_TIME = 5;
    private final static int VEL_JUMP = 8;
    private final static int VEL_WALK = 3;
    
    private final Profile prf;
    private final PlayerImages pi;
    private StateHandler stateHandler = NORMAL_HANDLER;
    private int runIndex = 0;
    private int runTimer = 0;
    private long lastShot = -1000;
    private long lastHurt = -1000;
    
    protected Player(final PlayerContext pc) {
        super(PLAYER_X, PLAYER_H);
        pc.player = this;
        this.prf = pc.prf;
        this.pi = pc.pi;
        registerInputs(pc.ctrl);
    }
    
    private final void registerInputs(final ControlScheme ctrl) {
        final Panput jumpInput = ctrl.get1();
        final Panput shootInput = null; //TODO
        register(jumpInput, new ActionStartListener() {
            @Override public final void onActionStart(final ActionStartEvent event) { jump(); }});
        register(jumpInput, new ActionEndListener() {
            @Override public final void onActionEnd(final ActionEndEvent event) { releaseJump(); }});
        register(shootInput, new ActionStartListener() {
            @Override public final void onActionStart(final ActionStartEvent event) { shoot(); }});
        register(shootInput, new ActionListener() {
            @Override public final void onAction(final ActionEvent event) { shooting(); }});
        register(shootInput, new ActionEndListener() {
            @Override public final void onActionEnd(final ActionEndEvent event) { releaseShoot(); }});
        register(ctrl.getRight(), new ActionListener() {
            @Override public final void onAction(final ActionEvent event) { right(); }});
        register(ctrl.getLeft(), new ActionListener() {
            @Override public final void onAction(final ActionEvent event) { left(); }});
        register(ctrl.getUp(), new ActionListener() { //TODO Display up/down touch buttons when near ladder, hide otherwise
            @Override public final void onAction(final ActionEvent event) { up(); }});
        register(ctrl.getDown(), new ActionListener() {
            @Override public final void onAction(final ActionEvent event) { down(); }});
        final Pangine engine = Pangine.getEngine();
        register(engine.getInteraction().KEY_F1, new ActionStartListener() {
            @Override public final void onActionStart(final ActionStartEvent event) { engine.captureScreen(); }});
    }
    
    private final void jump() {
        if (isGrounded()) {
            v = VEL_JUMP;
        }
    }
    
    private final void releaseJump() {
        if (v > 0) {
            v = 0;
        }
    }
    
    private final void shoot() {
        stateHandler.onShootStart(this);
    }
    
    private final void shooting() {
        stateHandler.onShooting(this);
    }
    
    private final void releaseShoot() {
        stateHandler.onShootEnd(this);
    }
    
    private final void right() {
        stateHandler.onRight(this);
    }
    
    private final void left() {
        stateHandler.onLeft(this);
    }
    
    private final void up() {
        stateHandler.onUp(this);
    }
    
    private final void down() {
        stateHandler.onDown(this);
    }
    
    private final boolean isHurt() {
        return (Pangine.getEngine().getClock() - lastHurt) < HURT_TIME;
    }
    
    private final boolean isInvincible() {
        return (Pangine.getEngine().getClock() - lastHurt) < INVINCIBLE_TIME;
    }
    
    private final PlayerImagesSubSet getCurrentImagesSubSet() {
        return ((Pangine.getEngine().getClock() - lastShot) < SHOOT_TIME) ? pi.shootSet : pi.basicSet;
    }
    
    private final void clearRun() {
        runIndex = 0;
        runTimer = 0;
    }
    
    @Override
    protected final boolean onStepCustom() {
        if (isInvincible()) {
            setVisible(Pangine.getEngine().isOn(4));
        } else {
            setVisible(true);
        }
        return false;
    }
    
    @Override
    protected final void onStepEnd() {
        hv = 0;
    }
    
    @Override
    protected final void onGrounded() {
        if (isHurt()) {
            changeView(pi.hurt);
            return;
        }
        final PlayerImagesSubSet set = getCurrentImagesSubSet();
        if (hv == 0) {
            changeView(set.stand);
            clearRun();
        } else {
            runTimer++;
            if (runTimer > RUN_TIME) {
                runTimer = 0;
                runIndex++;
                if (runIndex > 3) {
                    runIndex = 0;
                }
            }
            changeView(set.run[runIndex == 3 ? 1 : runIndex]);
        }
    }
    
    @Override
    protected final boolean onAir() {
        clearRun();
        if (isHurt()) {
            changeView(pi.hurt);
            return false;
        }
        changeView(getCurrentImagesSubSet().jump);
        return false;
    }
    
    @Override
    protected final boolean onFell() {
        return false;
    }

    @Override
    protected final void onBump(final int t) {
    }

    @Override
    protected final TileMap getTileMap() {
        return BotsnBoltsGame.tm;
    }

    @Override
    protected boolean isSolidBehavior(final byte b) {
        return false;
    }
    
    protected abstract static class StateHandler {
        protected abstract void onShootStart(final Player player);
        
        protected abstract void onShooting(final Player player);
        
        protected abstract void onShootEnd(final Player player);
        
        protected abstract void onRight(final Player player);
        
        protected abstract void onLeft(final Player player);
        
        //@OverrideMe
        protected void onUp(final Player player) {
        }
        
        //@OverrideMe
        protected void onDown(final Player player) {
        }
    }
    
    protected final static StateHandler NORMAL_HANDLER = new StateHandler() {
        @Override
        protected final void onShootStart(final Player player) {
            player.prf.shootMode.onShootStart(player);
        }
        
        @Override
        protected final void onShooting(final Player player) {
            player.prf.shootMode.onShooting(player);
        }
        
        @Override
        protected final void onShootEnd(final Player player) {
            player.prf.shootMode.onShootEnd(player);
        }
        
        @Override
        protected final void onRight(final Player player) {
            player.hv = VEL_WALK;
        }
        
        @Override
        protected final void onLeft(final Player player) {
            player.hv = -VEL_WALK;
        }
    };
    
    protected final static StateHandler LADDER_HANDLER = new StateHandler() {
        @Override
        protected final void onShootStart(final Player player) {
            player.prf.shootMode.onShootStart(player);
        }
        
        @Override
        protected final void onShooting(final Player player) {
            player.prf.shootMode.onShooting(player);
        }
        
        @Override
        protected final void onShootEnd(final Player player) {
            player.prf.shootMode.onShootEnd(player);
        }
        
        @Override
        protected final void onRight(final Player player) {
            //TODO Aim right
        }
        
        @Override
        protected final void onLeft(final Player player) {
            //TODO Aim left
        }
        
        @Override
        protected final void onUp(final Player player) {
            //TODO Climb up
        }
        
        @Override
        protected final void onDown(final Player player) {
        }
    };
    
    //protected final static StateHandler BALL_HANDLER = new StateHandler() { //TODO
    
    protected abstract static class ShootMode {
        protected final int delay;
        
        protected ShootMode(final int delay) {
            this.delay = delay;
        }
        
        protected abstract void onShootStart(final Player player);
        
        //@OverrideMe
        protected void onShooting(final Player player) {
        }
        
        //@OverrideMe
        protected void onShootEnd(final Player player) {
        }
        
        protected final void shoot(final Player player) {
            final long clock = Pangine.getEngine().getClock();
            if (clock - player.lastShot > delay) {
                player.lastShot = clock;
                createProjectile(player);
                //player.changeView(view);
            }
        }
        
        protected abstract void createProjectile(final Player player);
        
        protected final void createDefaultProjectile(final Player player) {
            new Projectile(player, 4, 0);
        }
    }
    
    protected final static class PlayerContext {
        protected final Profile prf;
        protected final ControlScheme ctrl;
        protected final PlayerImages pi;
        protected Player player = null;
        
        protected PlayerContext(final Profile prf, final ControlScheme ctrl, final PlayerImages pi) {
            this.prf = prf;
            this.ctrl = ctrl;
            this.pi = pi;
        }
    }
    
    protected final static class PlayerImages {
        private final PlayerImagesSubSet basicSet;
        private final PlayerImagesSubSet shootSet;
        private final Panmage hurt;
        
        protected PlayerImages(final PlayerImagesSubSet basicSet, final PlayerImagesSubSet shootSet, final Panmage hurt) {
            this.basicSet = basicSet;
            this.shootSet = shootSet;
            this.hurt = hurt;
        }
    }
    
    protected final static class PlayerImagesSubSet {
        private final Panmage stand;
        private final Panmage jump;
        private final Panmage[] run;
        
        protected PlayerImagesSubSet(final Panmage stand, final Panmage jump, final Panmage[] run) {
            this.stand = stand;
            this.jump = jump;
            this.run = run;
        }
    }
    
    protected final static ShootMode SHOOT_NORMAL = new ShootMode(SHOOT_DELAY_DEFAULT) {
        @Override
        protected final void onShootStart(final Player player) {
            shoot(player);
        }
        
        @Override
        protected final void createProjectile(final Player player) {
            createDefaultProjectile(player);
        }
    };
    
    protected final static ShootMode SHOOT_RAPID = new ShootMode(SHOOT_DELAY_RAPID) {
        @Override
        protected final void onShootStart(final Player player) {
        }
        
        @Override
        protected final void onShooting(final Player player) {
            shoot(player);
        }
        
        @Override
        protected final void createProjectile(final Player player) {
            createDefaultProjectile(player);
        }
    };
    
    protected final static ShootMode SHOOT_SPREAD = new ShootMode(SHOOT_DELAY_SPREAD) {
        @Override
        protected final void onShootStart(final Player player) {
            shoot(player);
        }
        
        @Override
        protected final void createProjectile(final Player player) {
            createDefaultProjectile(player);
            //TODO More shots
        }
    };
    
    protected final static ShootMode SHOOT_CHARGE = new ShootMode(SHOOT_DELAY_DEFAULT) {
        @Override
        protected final void onShootStart(final Player player) {
            shoot(player);
        }
        
        @Override
        protected final void onShootEnd(final Player player) {
            //TODO Shoot charged shot if ready
        }
        
        @Override
        protected final void createProjectile(final Player player) {
            createDefaultProjectile(player);
        }
    };
}
