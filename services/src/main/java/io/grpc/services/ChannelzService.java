/*
 * Copyright 2018, gRPC Authors All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.services;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.ExperimentalApi;
import io.grpc.Status;
import io.grpc.channelz.v1.ChannelzGrpc;
import io.grpc.channelz.v1.GetChannelRequest;
import io.grpc.channelz.v1.GetChannelResponse;
import io.grpc.channelz.v1.GetServerSocketsRequest;
import io.grpc.channelz.v1.GetServerSocketsResponse;
import io.grpc.channelz.v1.GetServersRequest;
import io.grpc.channelz.v1.GetServersResponse;
import io.grpc.channelz.v1.GetSocketRequest;
import io.grpc.channelz.v1.GetSocketResponse;
import io.grpc.channelz.v1.GetSubchannelRequest;
import io.grpc.channelz.v1.GetSubchannelResponse;
import io.grpc.channelz.v1.GetTopChannelsRequest;
import io.grpc.channelz.v1.GetTopChannelsResponse;
import io.grpc.internal.Channelz;
import io.grpc.internal.Channelz.ChannelStats;
import io.grpc.internal.Channelz.ServerList;
import io.grpc.internal.Channelz.SocketStats;
import io.grpc.internal.Instrumented;
import io.grpc.stub.StreamObserver;

/**
 * The channelz service provides stats about a running gRPC process.
 */
@ExperimentalApi("https://github.com/grpc/grpc-java/issues/4206")
public final class ChannelzService extends ChannelzGrpc.ChannelzImplBase {
  private final Channelz channelz;
  private final int maxPageSize;

  public ChannelzService newInstance(int maxPageSize) {
    return new ChannelzService(Channelz.instance(), maxPageSize);
  }

  @VisibleForTesting
  ChannelzService(Channelz channelz, int maxPageSize) {
    this.channelz = channelz;
    this.maxPageSize = maxPageSize;
  }

  /** Returns top level channel aka {@link io.grpc.internal.ManagedChannelImpl}. */
  @Override
  public void getTopChannels(
      GetTopChannelsRequest request, StreamObserver<GetTopChannelsResponse> responseObserver) {
    Channelz.RootChannelList rootChannels
        = channelz.getRootChannels(request.getStartChannelId(), maxPageSize);

    responseObserver.onNext(ChannelzProtoUtil.toGetTopChannelResponse(rootChannels));
    responseObserver.onCompleted();
  }

  /** Returns a top level channel aka {@link io.grpc.internal.ManagedChannelImpl}. */
  @Override
  public void getChannel(
      GetChannelRequest request, StreamObserver<GetChannelResponse> responseObserver) {
    Instrumented<ChannelStats> s = channelz.getRootChannel(request.getChannelId());
    if (s == null) {
      responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
      return;
    }

    responseObserver.onNext(
        GetChannelResponse
            .newBuilder()
            .setChannel(ChannelzProtoUtil.toChannel(s))
            .build());
    responseObserver.onCompleted();
  }

  /** Returns servers. */
  @Override
  public void getServers(
      GetServersRequest request, StreamObserver<GetServersResponse> responseObserver) {
    ServerList servers = channelz.getServers(request.getStartServerId(), maxPageSize);

    responseObserver.onNext(ChannelzProtoUtil.toGetServersResponse(servers));
    responseObserver.onCompleted();
  }

  /** Returns a subchannel. */
  @Override
  public void getSubchannel(
      GetSubchannelRequest request, StreamObserver<GetSubchannelResponse> responseObserver) {
    Instrumented<ChannelStats> s = channelz.getSubchannel(request.getSubchannelId());
    if (s == null) {
      responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
      return;
    }

    responseObserver.onNext(
        GetSubchannelResponse
            .newBuilder()
            .setSubchannel(ChannelzProtoUtil.toSubchannel(s))
            .build());
    responseObserver.onCompleted();
  }

  /** Returns a socket. */
  @Override
  public void getSocket(
      GetSocketRequest request, StreamObserver<GetSocketResponse> responseObserver) {
    Instrumented<SocketStats> s = channelz.getSocket(request.getSocketId());
    if (s == null) {
      responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
      return;
    }

    responseObserver.onNext(
          GetSocketResponse
              .newBuilder()
              .setSocket(ChannelzProtoUtil.toSocket(s))
              .build());
    responseObserver.onCompleted();
  }

  @Override
  public void getServerSockets(
      GetServerSocketsRequest request, StreamObserver<GetServerSocketsResponse> responseObserver) {
    // TODO(zpencer): fill this one out after refactoring channelz class
    responseObserver.onError(Status.UNIMPLEMENTED.asRuntimeException());
  }
}
