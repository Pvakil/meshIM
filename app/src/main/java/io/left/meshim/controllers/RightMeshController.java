package io.left.meshim.controllers;

import static io.left.rightmesh.mesh.MeshManager.ADDED;
import static io.left.rightmesh.mesh.MeshManager.DATA_RECEIVED;
import static io.left.rightmesh.mesh.MeshManager.PEER_CHANGED;
import static io.left.rightmesh.mesh.MeshManager.REMOVED;
import static protobuf.MeshIMMessages.MessageType.MESSAGE;
import static protobuf.MeshIMMessages.MessageType.PEER_UPDATE;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.google.protobuf.InvalidProtocolBufferException;

import io.left.meshim.R;
import io.left.meshim.activities.IActivity;
import io.left.meshim.database.MeshIMDao;
import io.left.meshim.database.MeshIMDatabase;
import io.left.meshim.models.MeshIDTuple;
import io.left.meshim.models.Message;
import io.left.meshim.models.User;
import io.left.meshim.services.MeshIMService;
import io.left.rightmesh.android.AndroidMeshManager;
import io.left.rightmesh.android.MeshService;
import io.left.rightmesh.id.MeshID;
import io.left.rightmesh.mesh.MeshManager.DataReceivedEvent;
import io.left.rightmesh.mesh.MeshManager.PeerChangedEvent;
import io.left.rightmesh.mesh.MeshManager.RightMeshEvent;
import io.left.rightmesh.mesh.MeshStateListener;
import io.left.rightmesh.util.RightMeshException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import protobuf.MeshIMMessages;
import protobuf.MeshIMMessages.MeshIMMessage;
import protobuf.MeshIMMessages.MessageType;
import protobuf.MeshIMMessages.PeerUpdate;

/**
 * All RightMesh logic abstracted into one class to keep it separate from Android logic.
 */
public class RightMeshController implements MeshStateListener {
    // Port to bind app to.
    private static final int MESH_PORT = 54321;

    // MeshManager instance - interface to the mesh network.
    private AndroidMeshManager meshManager = null;

    // Set to keep track of peers connected to the mesh.
    private HashSet<MeshID> discovered = new HashSet<>();
    private HashMap<MeshID, User> users = new HashMap<>();
    private User user = null;

    // Database interface.
    private MeshIMDao dao;

    // Link to current activity.
    private IActivity callback = null;
    //reference to service
    private MeshIMService meshIMService;

    /**
     * Constructor.
     * @param user user info for this device
     * @param dao DAO instance from open database connection
     * @param meshIMService link to service instance
     */
    public RightMeshController(User user, MeshIMDao dao,
                               MeshIMService meshIMService) {
        this.user = user;
        this.dao = dao;
        this.meshIMService = meshIMService;

        new Thread(() -> {
            if (dao.fetchAllUsers().length == 0) {
                // Insert this device's user as the first user on first run.
                this.dao.insertUsers(user);
            } else {
                // Otherwise make sure the database is up to date with SharedPreferences.
                this.dao.updateUsers(user);
            }
        }).start();
    }

    public void setCallback(IActivity callback) {
        this.callback = callback;
        updateInterface();
    }

    /**
     * Returns a list of online users.
     * @return online users
     */
    public List<User> getUserList() {
        return new ArrayList<>(users.values());
    }

    /**
     * Sends a simple text message to another user.
     * @param recipient recipient of the message
     * @param message contents of the message
     */
    public void sendTextMessage(User recipient, String message) {
        Message messageObject = new Message(user, recipient, message, true);
        try {
            byte[] messagePayload = createMessagePayloadFromMessage(messageObject);
            if (messagePayload != null) {
                meshManager.sendDataReliable(recipient.getMeshId(), MESH_PORT, messagePayload);
                dao.insertMessages(messageObject);
                updateInterface();
            }
        } catch (RightMeshException ignored) {
            // Something has gone wrong sending the message.
            // Don't store it in database or update UI.
        }
    }

    /**
     * Get a {@link AndroidMeshManager} instance, starting RightMesh if it isn't already running.
     *
     * @param context service context to bind to
     */
    public void connect(Context context) {
        meshManager = AndroidMeshManager.getInstance(context, RightMeshController.this,"Raturi");
    }

    /**
     * Close the RightMesh connection, stopping the service if no other apps are running.
     */
    public void disconnect() {
        try {
            if (meshManager != null) {
                meshManager.stop();
            }
        } catch (MeshService.ServiceDisconnectedException ignored) {
            // Error encountered shutting down service - nothing we can do from here.
        }
    }

    /**
     * Called by the {@link MeshService} when the mesh state changes. Initializes mesh connection
     * on first call.
     *
     * @param uuid  our own user id on first detecting
     * @param state state which indicates SUCCESS or an error code
     */
    @Override
    public void meshStateChanged(MeshID uuid, int state) {
        if (state == MeshStateListener.SUCCESS) {
            // Update stored user preferences with current MeshID.
            user.setMeshId(uuid);
            user.save();
            try {
                // Binds this app to MESH_PORT.
                // This app will now receive all events generated on that port.
                meshManager.bind(MESH_PORT);
            } catch (RightMeshException e) {
                // @TODO: App can't receive notifications. This needs to be alerted somehow.
            }

            // Subscribes handlers to receive events from the mesh.
            meshManager.on(DATA_RECEIVED, this::handleDataReceived);
            meshManager.on(PEER_CHANGED, this::handlePeerChanged);

            // Update the UI for the first time.
            updateInterface();
        }
    }

    /**
     * Exception boilerplate around {@link IActivity#updateInterface()}.
     */
    private void updateInterface() {
        try {
            if (callback != null) {
                callback.updateInterface();
            }
        } catch (RemoteException ignored) {
            // Connection to interface has broken - nothing we can do from here.
        }
    }

    /**
     * Handles incoming data events from the mesh - toasts the contents of the data.
     *
     * @param e event object from mesh
     */
    private void handleDataReceived(RightMeshEvent e) {
        DataReceivedEvent event = (DataReceivedEvent) e;

        try {
            MeshIMMessage messageWrapper = MeshIMMessage.parseFrom(event.data);
            MeshID peerId = event.peerUuid;

            if(peerId.equals(meshManager.getUuid())) {
                return;
            }

            MessageType messageType = messageWrapper.getMessageType();
            if (messageType == PEER_UPDATE) {
                Log.d("bug1","data recieved");
                PeerUpdate peerUpdate = messageWrapper.getPeerUpdate();

                // Initialize peer with info from update packet.
                User peer = new User(peerUpdate.getUserName(), peerUpdate.getAvatarId(), peerId);

                // Create or update user in database.
                MeshIDTuple dietPeer = dao.fetchMeshIdTupleByMeshId(peerId);
                if (dietPeer == null) {
                    dao.insertUsers(peer);

                    // Fetch the user's id after it is initialized.
                    dietPeer = dao.fetchMeshIdTupleByMeshId(peerId);
                    peer.id = dietPeer.id;
                } else {
                    peer.id = dietPeer.id;
                    dao.updateUsers(peer);
                }

                // Store user in list of online users.
                users.put(peerId, peer);
                updateInterface();
            } else if (messageType == MESSAGE) {
                MeshIMMessages.Message protoMessage = messageWrapper.getMessage();

                // Try to find user details, fetching from database if they aren't in the online
                // users list.
                User sender = users.get(peerId);
                if (sender == null) {
                    sender = dao.fetchUserByMeshId(peerId);
                }

                if (sender != null && user != null) {
                    Message message = new Message(sender, user, protoMessage.getMessage(), false);
                    dao.insertMessages(message);
                    meshIMService.sendNotification(sender, message);
                    updateInterface();
                }
            }
        } catch (InvalidProtocolBufferException ignored) { /* Ignore malformed messages. */ }
    }

    /**
     * Handles peer update events from the mesh - maintains a list of peers and updates the display.
     *
     * @param e event object from mesh
     */
    private void handlePeerChanged(RightMeshEvent e) {
        // Update peer list.
        PeerChangedEvent event = (PeerChangedEvent) e;

        // Ignore ourselves.
        if (event.peerUuid.equals(meshManager.getUuid())) {
            return;
        }

        if (event.state != REMOVED && !discovered.contains(event.peerUuid)) {
            discovered.add(event.peerUuid);
            Log.d("bug1","data changed");
            User tempUser = new User("Getting user details", R.mipmap.account_default);
            users.put(event.peerUuid,tempUser);
            updateInterface();
            // Send our information to a new or rejoining peer.
            byte[] message = createPeerUpdatePayloadFromUser(user);
            try {
                if (message != null) {
                    meshManager.sendDataReliable(event.peerUuid, MESH_PORT, message);
                    Log.d("bug1","data sent");
                }
            } catch (RightMeshException ignored) {
                // Message sending failed. Other user may have out of date information, but
                // ultimately this isn't deal-breaking.
            }
        } else if (event.state == REMOVED) {
            discovered.remove(event.peerUuid);
            users.remove(event.peerUuid);
            updateInterface();
        }
    }

    /**
     * Creates a byte array representing a {@link User}, to be broadcast over the mesh.
     * @param user user to be represented in bytes
     * @return payload to be broadcast
     */
    private byte[] createPeerUpdatePayloadFromUser(User user) {
        if (user == null) {
            return null;
        }

        PeerUpdate peerUpdate = PeerUpdate.newBuilder()
                .setUserName(user.getUsername())
                .setAvatarId(user.getAvatar())
                .build();

        MeshIMMessage message = MeshIMMessage.newBuilder()
                .setMessageType(PEER_UPDATE)
                .setPeerUpdate(peerUpdate)
                .build();

        return message.toByteArray();
    }

    /**
     * Creates a byte array representing a {@link Message}, to be broadcast over the mesh.
     * @param message message to be represented in bytes
     * @return payload to be broadcast
     */
    private byte[] createMessagePayloadFromMessage(Message message) {
        if (message == null) {
            return null;
        }

        MeshIMMessages.Message protoMsg = MeshIMMessages.Message.newBuilder()
                .setMessage(message.getMessage())
                .setTime(message.getDateAsTimestamp())
                .build();

        MeshIMMessage payload = MeshIMMessage.newBuilder()
                .setMessageType(MESSAGE)
                .setMessage(protoMsg)
                .build();

        return payload.toByteArray();
    }

    /**
     * Displays Rightmesh setting page.
     */
    public void showRightMeshSettings() {
        try {
            meshManager.showSettingsActivity();
        } catch (RightMeshException ignored) {
            // Service failed loading settings - nothing to be done.
        }
    }
}