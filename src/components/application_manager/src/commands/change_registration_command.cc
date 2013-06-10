/*

 Copyright (c) 2013, Ford Motor Company
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.

 Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following
 disclaimer in the documentation and/or other materials provided with the
 distribution.

 Neither the name of the Ford Motor Company nor the names of its contributors
 may be used to endorse or promote products derived from this software
 without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */

#include "application_manager/commands/change_registration_command.h"
#include "application_manager/message_chaining.h"
#include "application_manager/application_manager_impl.h"
#include "application_manager/application_impl.h"
#include "interfaces/v4_protocol_v2_0_revT.h"
#include "utils/file_system.h"

namespace application_manager {

namespace commands {

ChangeRegistrationCommand::ChangeRegistrationCommand(
    const MessageSharedPtr& message): CommandRequestImpl(message) {
}

ChangeRegistrationCommand::~ChangeRegistrationCommand() {
}

void ChangeRegistrationCommand::Run() {
  ApplicationImpl* app = static_cast<ApplicationImpl*>(
      ApplicationManagerImpl::instance()->
      application((*message_)[strings::params][strings::connection_key]));

  if (NULL == app) {
    SendResponse(false,
                 NsSmartDeviceLinkRPC::V2::Result::APPLICATION_NOT_REGISTERED);
    return;
  }

  // TODO(VS): Check supported languages in ApllicationManager

  const int corellation_id =
      (*message_)[strings::params][strings::correlation_id];
  const int connection_key =
      (*message_)[strings::params][strings::connection_key];

  MessageChaining * chain = NULL;


  bool has_actually_changed = false;
  if (app->ui_language() !=
     (*message_)[strings::msg_params][strings::hmi_display_language].asInt()) {
    smart_objects::CSmartObject* ui_request  = new smart_objects::CSmartObject();

    // TODO(VS): HMI Request Id
    const int ui_hmi_request_id = 210;
    (*ui_request)[strings::params][strings::function_id] =
        ui_hmi_request_id;

    (*ui_request)[strings::params][strings::message_type] =
        MessageType::kRequest;

    (*ui_request)[strings::msg_params][strings::language] =
        (*message_)[strings::msg_params][strings::hmi_display_language];

    (*ui_request)[strings::msg_params][strings::app_id] =
        app->app_id();

    chain = ApplicationManagerImpl::instance()->AddMessageChain(chain,
        connection_key, corellation_id, ui_hmi_request_id, &(*message_));

    ApplicationManagerImpl::instance()->SendMessageToHMI(ui_request);

    has_actually_changed = true;
  }

  if (app->language() !=
     (*message_)[strings::msg_params][strings::language].asInt()) {
    smart_objects::CSmartObject* vr_request  =
        new smart_objects::CSmartObject();

    // TODO(VS): HMI Request Id
    const int vr_hmi_request_id = 211;
    (*vr_request)[strings::params][strings::function_id] =
        vr_hmi_request_id;

    (*vr_request)[strings::params][strings::message_type] =
        MessageType::kRequest;

    (*vr_request)[strings::msg_params][strings::language] =
        (*message_)[strings::msg_params][strings::language];

    (*vr_request)[strings::msg_params][strings::app_id] =
        app->app_id();

    ApplicationManagerImpl::instance()->AddMessageChain(chain,
        connection_key, corellation_id, vr_hmi_request_id, &(*message_));

    ApplicationManagerImpl::instance()->SendMessageToHMI(vr_request);

    has_actually_changed = true;
  }

  if (!has_actually_changed) {
    SendResponse(true, NsSmartDeviceLinkRPC::V2::Result::SUCCESS);
  }
}

}  // namespace commands

}  // namespace application_manager
