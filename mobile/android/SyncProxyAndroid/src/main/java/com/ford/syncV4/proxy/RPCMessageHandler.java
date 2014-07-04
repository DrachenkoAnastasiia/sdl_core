package com.ford.syncV4.proxy;

import android.os.Handler;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.proxy.constants.Names;
import com.ford.syncV4.proxy.interfaces.IProxyListenerBase;
import com.ford.syncV4.proxy.rpc.AddCommandResponse;
import com.ford.syncV4.proxy.rpc.AddSubMenuResponse;
import com.ford.syncV4.proxy.rpc.AlertManeuverResponse;
import com.ford.syncV4.proxy.rpc.AlertResponse;
import com.ford.syncV4.proxy.rpc.ChangeRegistrationResponse;
import com.ford.syncV4.proxy.rpc.CreateInteractionChoiceSetResponse;
import com.ford.syncV4.proxy.rpc.DeleteCommandResponse;
import com.ford.syncV4.proxy.rpc.DeleteFileResponse;
import com.ford.syncV4.proxy.rpc.DeleteInteractionChoiceSetResponse;
import com.ford.syncV4.proxy.rpc.DeleteSubMenuResponse;
import com.ford.syncV4.proxy.rpc.DiagnosticMessageResponse;
import com.ford.syncV4.proxy.rpc.EncodedSyncPDataResponse;
import com.ford.syncV4.proxy.rpc.EndAudioPassThruResponse;
import com.ford.syncV4.proxy.rpc.GenericResponse;
import com.ford.syncV4.proxy.rpc.GetDTCsResponse;
import com.ford.syncV4.proxy.rpc.GetVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.ListFilesResponse;
import com.ford.syncV4.proxy.rpc.OnAppInterfaceUnregistered;
import com.ford.syncV4.proxy.rpc.OnAudioPassThru;
import com.ford.syncV4.proxy.rpc.OnButtonEvent;
import com.ford.syncV4.proxy.rpc.OnButtonPress;
import com.ford.syncV4.proxy.rpc.OnCommand;
import com.ford.syncV4.proxy.rpc.OnDriverDistraction;
import com.ford.syncV4.proxy.rpc.OnEncodedSyncPData;
import com.ford.syncV4.proxy.rpc.OnHMIStatus;
import com.ford.syncV4.proxy.rpc.OnHashChange;
import com.ford.syncV4.proxy.rpc.OnKeyboardInput;
import com.ford.syncV4.proxy.rpc.OnLanguageChange;
import com.ford.syncV4.proxy.rpc.OnPermissionsChange;
import com.ford.syncV4.proxy.rpc.OnSyncPData;
import com.ford.syncV4.proxy.rpc.OnTBTClientState;
import com.ford.syncV4.proxy.rpc.OnTouchEvent;
import com.ford.syncV4.proxy.rpc.OnVehicleData;
import com.ford.syncV4.proxy.rpc.PerformAudioPassThruResponse;
import com.ford.syncV4.proxy.rpc.PerformInteractionResponse;
import com.ford.syncV4.proxy.rpc.PutFileResponse;
import com.ford.syncV4.proxy.rpc.ReadDIDResponse;
import com.ford.syncV4.proxy.rpc.RegisterAppInterfaceResponse;
import com.ford.syncV4.proxy.rpc.ResetGlobalPropertiesResponse;
import com.ford.syncV4.proxy.rpc.ScrollableMessageResponse;
import com.ford.syncV4.proxy.rpc.SetAppIconResponse;
import com.ford.syncV4.proxy.rpc.SetDisplayLayoutResponse;
import com.ford.syncV4.proxy.rpc.SetGlobalPropertiesResponse;
import com.ford.syncV4.proxy.rpc.SetMediaClockTimerResponse;
import com.ford.syncV4.proxy.rpc.ShowConstantTBTResponse;
import com.ford.syncV4.proxy.rpc.ShowResponse;
import com.ford.syncV4.proxy.rpc.SliderResponse;
import com.ford.syncV4.proxy.rpc.SpeakResponse;
import com.ford.syncV4.proxy.rpc.SubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.SubscribeVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.SyncPDataResponse;
import com.ford.syncV4.proxy.rpc.SystemRequestResponse;
import com.ford.syncV4.proxy.rpc.UnsubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.UnsubscribeVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.UpdateTurnListResponse;
import com.ford.syncV4.proxy.rpc.enums.AppInterfaceUnregisteredReason;
import com.ford.syncV4.proxy.rpc.enums.HMILevel;
import com.ford.syncV4.proxy.rpc.enums.SyncConnectionState;
import com.ford.syncV4.proxy.rpc.enums.SyncDisconnectedReason;
import com.ford.syncV4.transport.TransportType;
import com.ford.syncV4.util.logger.Logger;

import java.util.Hashtable;

/**
 * Created by Andrew Batutin on 2/10/14
 */
public class RPCMessageHandler implements IRPCMessageHandler {

    private static final String CLASS_NAME = RPCMessageHandler.class.getSimpleName();

    private SyncProxyBase syncProxyBase;

    public RPCMessageHandler(SyncProxyBase syncProxyBase) {
        this.syncProxyBase = syncProxyBase;
    }

    @Override
    public void handleRPCMessage(byte sessionId, String appId, Hashtable hash) {
        if (hash != null) {
            handleRPCMessageInt(sessionId, appId, hash);
        }
    }

    private void handleRPCMessageInt(final byte sessionId, final String appId, Hashtable hash) {
        RPCMessage rpcMsg = new RPCMessage(hash);
        String functionName = rpcMsg.getFunctionName();
        String messageType = rpcMsg.getMessageType();

        if (messageType.equals(Names.response)) {
            Logger.d(CLASS_NAME + " Response name:" + functionName);

            final RPCResponse response = new RPCResponse(hash);
            final Integer responseCorrelationID = response.getCorrelationId();

            if (!syncProxyBase.handlePartialRPCResponse(sessionId, response, hash) &&
                    !syncProxyBase.handleLastInternalResponse(response)) {

                // Check to ensure response is not from an internal message (reserved correlation ID)
                if (syncProxyBase.isCorrelationIDProtected(responseCorrelationID)) {
                    // This is a response generated from an internal message, it can be trapped here
                    // The app should not receive a response for a request it did not send
                    if (syncProxyBase.isRegisterAppInterfaceCorrelationIdProtected(responseCorrelationID) &&
                            syncProxyBase.getAdvancedLifecycleManagementEnabled() &&
                            functionName.equals(Names.RegisterAppInterface)) {
                        final RegisterAppInterfaceResponse msg = new RegisterAppInterfaceResponse(hash);
                        if (msg.getSuccess()) {
                            syncProxyBase.setAppInterfaceRegistered(sessionId, true);
                        }

                        //_autoActivateIdReturned = msg.getAutoActivateID();
                        /*Place holder for legacy support*/
                        syncProxyBase.setAutoActivateIdReturned("8675309");
                        syncProxyBase.setButtonCapabilities(msg.getButtonCapabilities());
                        syncProxyBase.setDisplayCapabilities(msg.getDisplayCapabilities());
                        syncProxyBase.setSoftButtonCapabilities(msg.getSoftButtonCapabilities());
                        syncProxyBase.setPresetBankCapabilities(msg.getPresetBankCapabilities());
                        syncProxyBase.setHmiZoneCapabilities(msg.getHmiZoneCapabilities());
                        syncProxyBase.setSpeechCapabilities(msg.getSpeechCapabilities());
                        syncProxyBase.setSyncLanguage(msg.getLanguage());
                        syncProxyBase.setHmiDisplayLanguage(msg.getHmiDisplayLanguage());
                        syncProxyBase.setSyncMsgVersion(msg.getSyncMsgVersion());
                        syncProxyBase.setVrCapabilities(msg.getVrCapabilities());
                        syncProxyBase.setVehicleType(msg.getVehicleType());
                        syncProxyBase.setSyncConnectionState(SyncConnectionState.SYNC_CONNECTED);

                        // If registerAppInterface failed, exit with OnProxyUnusable
                        /*if (!msg.getSuccess()) {
                            String errorMsg = "Unable to register app interface. Review values" +
                                    "passed to the SyncProxy constructor." +
                                    "RegisterAppInterface result code: ";
                            syncProxyBase.notifyProxyClosed(errorMsg,
                                    new SyncException(errorMsg + msg.getResultCode(),
                                            SyncExceptionCause.SYNC_REGISTRATION_ERROR));
                        }*/
                        syncProxyBase.processRegisterAppInterfaceResponse(sessionId, msg);
                    } else if (syncProxyBase.isPolicyCorrelationIdProtected(responseCorrelationID) &&
                                    functionName.equals(Names.OnEncodedSyncPData)) {
                        // OnEncodedSyncPData
                        final OnEncodedSyncPData msg = new OnEncodedSyncPData(hash);
                        // If url is null, then send notification to the app, otherwise, send to URL
                        if (msg.getUrl() != null) {
                            // URL has data, attempt to post request to external server
                            Thread handleOffboardSyncTransmissionTread =
                                    new Thread() {
                                        @Override
                                        public void run() {
                                            syncProxyBase.sendEncodedSyncPDataToUrl(appId,
                                                    msg.getUrl(), msg.getData(), msg.getTimeout());
                                        }
                                    };

                            handleOffboardSyncTransmissionTread.start();
                        }
                    } else if (syncProxyBase.isUnregisterAppInterfaceCorrelationIdProtected(responseCorrelationID) &&
                            functionName.equals(Names.UnregisterAppInterface)) {
                        syncProxyBase.onUnregisterAppInterfaceResponse(sessionId, hash);
                    }
                    return;
                }

                if (functionName.equals(Names.RegisterAppInterface)) {
                    final RegisterAppInterfaceResponse msg = new RegisterAppInterfaceResponse(hash);
                    if (msg.getSuccess()) {
                        syncProxyBase.setAppInterfaceRegistered(sessionId, true);
                    }

                    //_autoActivateIdReturned = msg.getAutoActivateID();
                    /*Place holder for legacy support*/

                    syncProxyBase.setAutoActivateIdReturned("8675309");
                    syncProxyBase.setButtonCapabilities(msg.getButtonCapabilities());
                    syncProxyBase.setDisplayCapabilities(msg.getDisplayCapabilities());
                    syncProxyBase.setSoftButtonCapabilities(msg.getSoftButtonCapabilities());
                    syncProxyBase.setPresetBankCapabilities(msg.getPresetBankCapabilities());
                    syncProxyBase.setHmiZoneCapabilities(msg.getHmiZoneCapabilities());
                    syncProxyBase.setSpeechCapabilities(msg.getSpeechCapabilities());
                    syncProxyBase.setSyncLanguage(msg.getLanguage());
                    syncProxyBase.setHmiDisplayLanguage(msg.getHmiDisplayLanguage());
                    syncProxyBase.setSyncMsgVersion(msg.getSyncMsgVersion());
                    syncProxyBase.setVrCapabilities(msg.getVrCapabilities());
                    syncProxyBase.setVehicleType(msg.getVehicleType());


                    // RegisterAppInterface
                    /*if (syncProxyBase.getAdvancedLifecycleManagementEnabled()) {
                        syncProxyBase.setSyncConnectionState(SyncConnectionState.SYNC_CONNECTED);

                        // If registerAppInterface failed, exit with OnProxyUnusable
                        if (!msg.getSuccess()) {
                            String errorMsg = "Unable to register app interface. Review values " +
                                    "passed to the SyncProxy constructor. RegisterAppInterface " +
                                    "result code: ";
                            syncProxyBase.notifyProxyClosed(errorMsg,
                                    new SyncException(errorMsg + msg.getResultCode(),
                                            SyncExceptionCause.SYNC_REGISTRATION_ERROR
                                    )
                            );
                        }
                    }*/
                    syncProxyBase.processRegisterAppInterfaceResponse(sessionId, msg);
                } else if (functionName.equals(Names.Speak)) {
                    // SpeakResponse

                    final SpeakResponse msg = new SpeakResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onSpeakResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onSpeakResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.Alert)) {
                    // AlertResponse

                    final AlertResponse msg = new AlertResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onAlertResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onAlertResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.Show)) {
                    // ShowResponse

                    final ShowResponse msg = new ShowResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onShowResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onShowResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.DiagnosticMessage)) {
                    // AddCommand
                    final DiagnosticMessageResponse msg = new DiagnosticMessageResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onDiagnosticMessageResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onDiagnosticMessageResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.AddCommand)) {
                    // AddCommand
                    final AddCommandResponse msg = new AddCommandResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onAddCommandResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onAddCommandResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.DeleteCommand)) {
                    // DeleteCommandResponse

                    final DeleteCommandResponse msg =
                            new DeleteCommandResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onDeleteCommandResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onDeleteCommandResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.AddSubMenu)) {
                    // AddSubMenu

                    final AddSubMenuResponse msg = new AddSubMenuResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onAddSubMenuResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onAddSubMenuResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.DeleteSubMenu)) {
                    // DeleteSubMenu

                    final DeleteSubMenuResponse msg =
                            new DeleteSubMenuResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onDeleteSubMenuResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onDeleteSubMenuResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.SubscribeButton)) {
                    // SubscribeButton

                    final SubscribeButtonResponse msg =
                            new SubscribeButtonResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onSubscribeButtonResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onSubscribeButtonResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.UnsubscribeButton)) {
                    // UnsubscribeButton

                    final UnsubscribeButtonResponse msg =
                            new UnsubscribeButtonResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onUnsubscribeButtonResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onUnsubscribeButtonResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.SetMediaClockTimer)) {
                    // SetMediaClockTimer

                    final SetMediaClockTimerResponse msg =
                            new SetMediaClockTimerResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onSetMediaClockTimerResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onSetMediaClockTimerResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.EncodedSyncPData)) {
                    // EncodedSyncPData

                    final EncodedSyncPDataResponse msg =
                            new EncodedSyncPDataResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onEncodedSyncPDataResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onEncodedSyncPDataResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.SyncPData)) {
                    // SyncPData

                    final SyncPDataResponse msg = new SyncPDataResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onSyncPDataResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onSyncPDataResponse(appId, msg);
                    }
                } else if (functionName.equals(
                        Names.CreateInteractionChoiceSet)) {
                    // CreateInteractionChoiceSet

                    final CreateInteractionChoiceSetResponse msg =
                            new CreateInteractionChoiceSetResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onCreateInteractionChoiceSetResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onCreateInteractionChoiceSetResponse(appId, msg);
                    }
                } else if (functionName.equals(
                        Names.DeleteInteractionChoiceSet)) {
                    // DeleteInteractionChoiceSet

                    final DeleteInteractionChoiceSetResponse msg =
                            new DeleteInteractionChoiceSetResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onDeleteInteractionChoiceSetResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onDeleteInteractionChoiceSetResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.PerformInteraction)) {
                    // PerformInteraction

                    final PerformInteractionResponse msg =
                            new PerformInteractionResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onPerformInteractionResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onPerformInteractionResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.SetGlobalProperties)) {
                    final SetGlobalPropertiesResponse msg =
                            new SetGlobalPropertiesResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onSetGlobalPropertiesResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onSetGlobalPropertiesResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.ResetGlobalProperties)) {
                    // ResetGlobalProperties

                    final ResetGlobalPropertiesResponse msg =
                            new ResetGlobalPropertiesResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onResetGlobalPropertiesResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onResetGlobalPropertiesResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.UnregisterAppInterface)) {
                    syncProxyBase.onUnregisterAppInterfaceResponse(sessionId, hash);
                } else if (functionName.equals(Names.GenericResponse)) {
                    // GenericResponse (Usually and error)
                    final GenericResponse msg = new GenericResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onGenericResponse(msg);
                            }
                        });
                    } else {
                        getProxyListener().onGenericResponse(msg);
                    }
                } else if (functionName.equals(Names.Slider)) {
                    // Slider
                    final SliderResponse msg = new SliderResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onSliderResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onSliderResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.PutFile)) {
                    // PutFile
                    final PutFileResponse msg = new PutFileResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onPutFileResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onPutFileResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.DeleteFile)) {
                    // DeleteFile
                    final DeleteFileResponse msg = new DeleteFileResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onDeleteFileResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onDeleteFileResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.ListFiles)) {
                    // ListFiles
                    final ListFilesResponse msg = new ListFilesResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onListFilesResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onListFilesResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.SetAppIcon)) {
                    // SetAppIcon
                    final SetAppIconResponse msg = new SetAppIconResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onSetAppIconResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onSetAppIconResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.ScrollableMessage)) {
                    // ScrollableMessage
                    final ScrollableMessageResponse msg =
                            new ScrollableMessageResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onScrollableMessageResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onScrollableMessageResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.ChangeRegistration)) {
                    // ChangeLanguageRegistration
                    final ChangeRegistrationResponse msg =
                            new ChangeRegistrationResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onChangeRegistrationResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onChangeRegistrationResponse(appId,  msg);
                    }
                } else if (functionName.equals(Names.SetDisplayLayout)) {
                    // SetDisplayLayout
                    final SetDisplayLayoutResponse msg =
                            new SetDisplayLayoutResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onSetDisplayLayoutResponse(appId,
                                        msg);
                            }
                        });
                    } else {
                        getProxyListener().onSetDisplayLayoutResponse(appId,
                                msg);
                    }
                } else if (functionName.equals(Names.PerformAudioPassThru)) {
                    // PerformAudioPassThru
                    final PerformAudioPassThruResponse msg =
                            new PerformAudioPassThruResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onPerformAudioPassThruResponse(appId,
                                        msg);
                            }
                        });
                    } else {
                        getProxyListener().onPerformAudioPassThruResponse(appId,
                                msg);
                    }
                } else if (functionName.equals(Names.EndAudioPassThru)) {
                    // EndAudioPassThru
                    final EndAudioPassThruResponse msg =
                            new EndAudioPassThruResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onEndAudioPassThruResponse(appId,
                                        msg);
                            }
                        });
                    } else {
                        getProxyListener().onEndAudioPassThruResponse(appId,
                                msg);
                    }
                } else if (functionName.equals(Names.SubscribeVehicleData)) {
                    // SubscribeVehicleData
                    final SubscribeVehicleDataResponse msg =
                            new SubscribeVehicleDataResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onSubscribeVehicleDataResponse(appId,
                                        msg);
                            }
                        });
                    } else {
                        getProxyListener().onSubscribeVehicleDataResponse(appId,
                                msg);
                    }
                } else if (functionName.equals(Names.UnsubscribeVehicleData)) {
                    // UnsubscribeVehicleData
                    final UnsubscribeVehicleDataResponse msg =
                            new UnsubscribeVehicleDataResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onUnsubscribeVehicleDataResponse(appId,
                                        msg);
                            }
                        });
                    } else {
                        getProxyListener().onUnsubscribeVehicleDataResponse(appId,
                                msg);
                    }
                } else if (functionName.equals(Names.GetVehicleData)) {
                    // GetVehicleData
                    final GetVehicleDataResponse msg =
                            new GetVehicleDataResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onGetVehicleDataResponse(appId,
                                        msg);
                            }
                        });
                    } else {
                        getProxyListener().onGetVehicleDataResponse(appId,
                                msg);
                    }
                } else if (functionName.equals(Names.ReadDID)) {
                    // ReadDID
                    final ReadDIDResponse msg = new ReadDIDResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onReadDIDResponse(appId,
                                        msg);
                            }
                        });
                    } else {
                        getProxyListener().onReadDIDResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.GetDTCs)) {
                    // GetDTCs
                    final GetDTCsResponse msg = new GetDTCsResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onGetDTCsResponse(appId,
                                        msg);
                            }
                        });
                    } else {
                        getProxyListener().onGetDTCsResponse(appId, msg);
                    }
                } else if (functionName.equals(Names.AlertManeuver)) {
                    // AlertManeuver
                    final AlertManeuverResponse msg =
                            new AlertManeuverResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onAlertManeuverResponse(appId,
                                        msg);
                            }
                        });
                    } else {
                        getProxyListener().onAlertManeuverResponse(appId,
                                msg);
                    }
                } else if (functionName.equals(Names.ShowConstantTBT)) {
                    // ShowConstantTBT
                    final ShowConstantTBTResponse msg =
                            new ShowConstantTBTResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onShowConstantTBTResponse(appId,
                                        msg);
                            }
                        });
                    } else {
                        getProxyListener().onShowConstantTBTResponse(appId,
                                msg);
                    }
                } else if (functionName.equals(Names.UpdateTurnList)) {
                    // UpdateTurnList
                    final UpdateTurnListResponse msg =
                            new UpdateTurnListResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onUpdateTurnListResponse(appId,
                                        msg);
                            }
                        });
                    } else {
                        getProxyListener().onUpdateTurnListResponse(appId,
                                msg);
                    }
                } else if (functionName.equals(Names.SystemRequest)) {
                    final SystemRequestResponse msg =
                            new SystemRequestResponse(hash);
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onSystemRequestResponse(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onSystemRequestResponse(appId, msg);
                    }
                } else {
                    try {
                        if (syncProxyBase.getSyncMsgVersion() != null) {
                            Logger.e("Unrecognized response Message: " +
                                    functionName +
                                    "SYNC Message Version = " + syncProxyBase.getSyncMsgVersion());
                        } else {
                            Logger.e("Unrecognized response Message: " + functionName);
                        }
                    } catch (SyncException e) {
                        e.printStackTrace();
                    }
                } // end-if

            }
        } else if (messageType.equals(Names.notification)) {
            Logger.d(CLASS_NAME + " Notification name:" + functionName);
            if (functionName.equals(Names.OnHMIStatus)) {
                // OnHMIStatus

                final OnHMIStatus msg = new OnHMIStatus(hash);
                msg.setFirstRun(new Boolean(syncProxyBase.getFirstTimeFull()));
                if (msg.getHmiLevel() == HMILevel.HMI_FULL) syncProxyBase.setFirstTimeFull(false);

                if (msg.getHmiLevel() != syncProxyBase.getPriorHmiLevel() && msg.getAudioStreamingState() != syncProxyBase.getPriorAudioStreamingState()) {
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onOnHMIStatus(appId, msg);
                            }
                        });
                    } else {
                        getProxyListener().onOnHMIStatus(appId, msg);
                    }
                }
            } else if (functionName.equals(Names.OnCommand)) {
                // OnCommand

                final OnCommand msg = new OnCommand(hash);
                if (getCallbackToUIThread()) {
                    // Run in UI thread
                    getMainUIHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            getProxyListener().onOnCommand(appId, msg);
                        }
                    });
                } else {
                    getProxyListener().onOnCommand(appId, msg);
                }
            } else if (functionName.equals(Names.OnDriverDistraction)) {
                // OnDriverDistration

                final OnDriverDistraction msg = new OnDriverDistraction(hash);
                if (getCallbackToUIThread()) {
                    // Run in UI thread
                    getMainUIHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            getProxyListener().onOnDriverDistraction(msg);
                        }
                    });
                } else {
                    getProxyListener().onOnDriverDistraction(msg);
                }
            } else if (functionName.equals(Names.OnEncodedSyncPData)) {
                // OnEncodedSyncPData

                final OnEncodedSyncPData msg = new OnEncodedSyncPData(hash);

                // If url is null, then send notification to the app, otherwise, send to URL
                if (msg.getUrl() == null) {
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onOnEncodedSyncPData(msg);
                            }
                        });
                    } else {
                        getProxyListener().onOnEncodedSyncPData(msg);
                    }
                } else {
                    // URL has data, attempt to post request to external server
                    Thread handleOffboardSyncTransmissionTread = new Thread() {
                        @Override
                        public void run() {
                            syncProxyBase.sendEncodedSyncPDataToUrl(appId, msg.getUrl(),
                                    msg.getData(), msg.getTimeout());
                        }
                    };

                    handleOffboardSyncTransmissionTread.start();
                }
            } else if (functionName.equals(Names.OnSyncPData)) {
                // OnSyncPData
                final OnSyncPData msg = new OnSyncPData(hash);

                // If url is null, then send notification to the app, otherwise, send to URL
                if (msg.getUrl() == null) {
                    if (getCallbackToUIThread()) {
                        // Run in UI thread
                        getMainUIHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                getProxyListener().onOnSyncPData(msg);
                            }
                        });
                    } else {
                        getProxyListener().onOnSyncPData(msg);
                    }
                } else { //url not null, send to url
                    // URL has data, attempt to post request to external server
                    Thread handleOffboardSyncTransmissionTread = new Thread() {
                        @Override
                        public void run() {
                            syncProxyBase.sendSyncPDataToUrl(appId, msg.getUrl(),
                                    msg.getSyncPData(), msg.getTimeout());
                        }
                    };

                    handleOffboardSyncTransmissionTread.start();
                }
            } else if (functionName.equals(Names.OnPermissionsChange)) {
                //OnPermissionsChange

                final OnPermissionsChange msg = new OnPermissionsChange(hash);
                if (getCallbackToUIThread()) {
                    // Run in UI thread
                    getMainUIHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            getProxyListener().onOnPermissionsChange(appId, msg);
                        }
                    });
                } else {
                    getProxyListener().onOnPermissionsChange(appId, msg);
                }
            } else if (functionName.equals(Names.OnTBTClientState)) {
                // OnTBTClientState

                final OnTBTClientState msg = new OnTBTClientState(hash);
                if (getCallbackToUIThread()) {
                    // Run in UI thread
                    getMainUIHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            getProxyListener().onOnTBTClientState(msg);
                        }
                    });
                } else {
                    getProxyListener().onOnTBTClientState(msg);
                }
            } else if (functionName.equals(Names.OnButtonPress)) {
                // OnButtonPress

                final OnButtonPress msg = new OnButtonPress(hash);
                if (getCallbackToUIThread()) {
                    // Run in UI thread
                    getMainUIHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            getProxyListener().onOnButtonPress(appId, msg);
                        }
                    });
                } else {
                    getProxyListener().onOnButtonPress(appId, msg);
                }
            } else if (functionName.equals(Names.OnButtonEvent)) {
                // OnButtonEvent

                final OnButtonEvent msg = new OnButtonEvent(hash);
                if (getCallbackToUIThread()) {
                    // Run in UI thread
                    getMainUIHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            getProxyListener().onOnButtonEvent(appId, msg);
                        }
                    });
                } else {
                    getProxyListener().onOnButtonEvent(appId, msg);
                }
            } else if (functionName.equals(Names.OnLanguageChange)) {
                // OnLanguageChange

                final OnLanguageChange msg = new OnLanguageChange(hash);
                syncProxyBase.setLastLanguageChange(msg);
                if (getCallbackToUIThread()) {
                    // Run in UI thread
                    getMainUIHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            getProxyListener().onOnLanguageChange(appId, msg);
                        }
                    });
                } else {
                    getProxyListener().onOnLanguageChange(appId, msg);
                }
            } else if (functionName.equals(Names.OnAudioPassThru)) {
                // OnAudioPassThru
                final OnAudioPassThru msg = new OnAudioPassThru(hash);
                if (getCallbackToUIThread()) {
                    // Run in UI thread
                    getMainUIHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            getProxyListener().onOnAudioPassThru(appId, msg);
                        }
                    });
                } else {
                    getProxyListener().onOnAudioPassThru(appId, msg);
                }
            } else if (functionName.equals(Names.OnVehicleData)) {
                // OnVehicleData
                final OnVehicleData msg = new OnVehicleData(hash);
                if (getCallbackToUIThread()) {
                    // Run in UI thread
                    getMainUIHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            getProxyListener().onOnVehicleData(appId, msg);
                        }
                    });
                } else {
                    getProxyListener().onOnVehicleData(appId, msg);
                }
            } else if (functionName.equals(Names.OnTouchEvent)) {
                // OnTouchEvent
                final OnTouchEvent msg = new OnTouchEvent(hash);
                if (getCallbackToUIThread()) {
                    // Run in UI thread
                    getMainUIHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            getProxyListener().onOnTouchEvent(appId, msg);
                        }
                    });
                } else {
                    getProxyListener().onOnTouchEvent(appId, msg);
                }
            } else if (functionName.equals(Names.OnKeyboardInput)) {
                // OnKeyboardInput
                final OnKeyboardInput msg = new OnKeyboardInput(hash);
                if (getCallbackToUIThread()) {
                    // Run in UI thread
                    getMainUIHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            getProxyListener().onKeyboardInput(appId, msg);
                        }
                    });
                } else {
                    getProxyListener().onKeyboardInput(appId, msg);
                }
            } else if (functionName.equals(Names.OnSystemRequest)) {
                syncProxyBase.handleOnSystemRequest(appId, hash);
            } else if (functionName.equals(Names.OnAppInterfaceUnregistered)) {
                // OnAppInterfaceUnregistered
                syncProxyBase.setAppInterfaceRegistered(sessionId, false);
                synchronized (syncProxyBase.APP_INTERFACE_LOCK) {
                    syncProxyBase.APP_INTERFACE_LOCK.notify();
                }

                final OnAppInterfaceUnregistered msg = new OnAppInterfaceUnregistered(hash);

                if (syncProxyBase.getAdvancedLifecycleManagementEnabled()) {
                    if (msg.getReason() == AppInterfaceUnregisteredReason.LANGUAGE_CHANGE) {
                        if (getCallbackToUIThread()) {
                            // Run in UI thread
                            getMainUIHandler().post(new Runnable() {
                                @Override
                                public void run() {
                                    getProxyListener()
                                            .onAppUnregisteredAfterLanguageChange(appId,
                                                    syncProxyBase.getLastLanguageChange());
                                }
                            });
                        } else {
                            getProxyListener().onAppUnregisteredAfterLanguageChange(appId,
                                    syncProxyBase.getLastLanguageChange());
                        }
                    } else if (msg.getReason() != null) {
                        syncProxyBase.onAppUnregisteredReason(appId, msg);
                    } else {
                        // This requires the proxy to be cycled
                        if (syncProxyBase.getCurrentTransportType() == TransportType.BLUETOOTH) {
                            syncProxyBase.cycleProxy(SyncDisconnectedReason.convertAppInterfaceUnregisteredReason(msg.getReason()));
                        } else {
                            Logger.e(CLASS_NAME + " HandleRPCMessage. No cycle required if transport is TCP");
                        }
                        syncProxyBase.notifyOnAppInterfaceUnregistered(msg);
                    }
                } else {
                    syncProxyBase.notifyOnAppInterfaceUnregistered(msg);
                }
            } else if (functionName.equals(Names.OnHashChange)) {
                // OnHashChange
                final OnHashChange onHashChange = new OnHashChange(hash);

                syncProxyBase.setHashId(appId, onHashChange.getHashID());

                if (getCallbackToUIThread()) {
                    // Run in UI thread
                    getMainUIHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            getProxyListener().onHashChange(appId, onHashChange);
                        }
                    });
                } else {
                    getProxyListener().onHashChange(appId, onHashChange);
                }
            } else {
                try {
                    if (syncProxyBase.getSyncMsgVersion() != null) {
                        Logger.i("Unrecognized notification Message: " + functionName +
                                " connected to SYNC using message version: " +
                                syncProxyBase.getSyncMsgVersion().getMajorVersion() + "." +
                                syncProxyBase.getSyncMsgVersion().getMinorVersion());
                    } else {
                        Logger.i("Unrecognized notification Message: " + functionName);
                    }
                } catch (SyncException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Handler getMainUIHandler() {
        return syncProxyBase.getMainUIHandler();
    }

    private IProxyListenerBase getProxyListener() {
        return syncProxyBase.getProxyListener();
    }

    private Boolean getCallbackToUIThread() {
        return syncProxyBase.getCallbackToUIThread();
    }
}