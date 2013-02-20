/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.component.id;

import java.util.ArrayList;
import java.util.List;
import org.jboss.netty.channel.ChannelPipelineFactory;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentId.FaultLogPrefix;
import com.eucalyptus.component.ComponentId.Partition;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.util.Internets;

@Partition( value = { Eucalyptus.class } )
@FaultLogPrefix( "cloud" ) // stub for cc, but in clc
public class ClusterController extends ComponentId {
  
  private static final long       serialVersionUID = 1L;
  public static final ComponentId INSTANCE         = new ClusterController( );
  
  public ClusterController( ) {
    super( "cluster" );
  }
  
  @Override
  public Integer getPort( ) {
    return 8774;
  }
  
  @Override
  public String getLocalEndpointName( ) {
    return ServiceUris.remote( this, Internets.localHostInetAddress( ), this.getPort( ) ).toASCIIString( );
  }
  
  private static ChannelPipelineFactory clusterPipeline;
  
  @Override
  public ChannelPipelineFactory getClientPipeline( ) {//TODO:GRZE:fixme to use discovery
    ChannelPipelineFactory factory = null;
    if ( ( factory = super.getClientPipeline( ) ) == null ) {
      factory = ( clusterPipeline = ( clusterPipeline != null
        ? clusterPipeline
          : helpGetClientPipeline( "com.eucalyptus.ws.client.pipeline.ClusterClientPipelineFactory" ) ) );
    }
    return factory;
  }
  
  @Override
  public String getServicePath( final String... pathParts ) {
    return "/axis2/services/EucalyptusCC";
  }
  
  @Override
  public String getInternalServicePath( final String... pathParts ) {
    return this.getServicePath( pathParts );
  }
  
  @Partition( value = { ClusterController.class }, manyToOne = true )
  @InternalService
  @FaultLogPrefix( "cloud" ) // nc stub, but within clc
  public static class NodeController extends ComponentId {
    
    public NodeController( ) {
      super( "node" );
    }
    
    @Override
    public Integer getPort( ) {
      return 8775;
    }
    
    @Override
    public String getLocalEndpointName( ) {
      return ServiceUris.remote( this, Internets.localHostInetAddress( ), this.getPort( ) ).toASCIIString( );
    }
    
    @Override
    public String getServicePath( final String... pathParts ) {
      return "/axis2/services/EucalyptusNC";
    }
    
    @Override
    public String getInternalServicePath( final String... pathParts ) {
      return this.getServicePath( pathParts );
    }
    
    @Override
    public boolean isRegisterable( ) {
      return false;
    }

  }
  
  @Partition( ClusterController.class )
  @InternalService
  @FaultLogPrefix( "cloud" )
  public static class GatherLogService extends ComponentId {
    
    private static final long serialVersionUID = 1L;
    
    public GatherLogService( ) {
      super( "gatherlog" );
    }
    
    @Override
    public Integer getPort( ) {
      return 8774;
    }
    
    @Override
    public String getLocalEndpointName( ) {
      return ServiceUris.remote( this, Internets.localHostInetAddress( ), this.getPort( ) ).toASCIIString( );
    }
    
    @Override
    public String getServicePath( final String... pathParts ) {
      return "/axis2/services/EucalyptusGL";
    }
    
    @Override
    public String getInternalServicePath( final String... pathParts ) {
      return this.getServicePath( pathParts );
    }
    
    private static ChannelPipelineFactory logPipeline;
    
    /**
     * This was born under a bad sign. No touching.
     * 
     * @return
     */
    @Override
    public ChannelPipelineFactory getClientPipeline( ) {
      ChannelPipelineFactory factory = null;
      if ( ( factory = super.getClientPipeline( ) ) == null ) {
        factory = ( logPipeline = ( logPipeline != null
          ? logPipeline
          : helpGetClientPipeline( "com.eucalyptus.ws.client.pipeline.GatherLogClientPipeline" ) ) );
      }
      return factory;
    }
    
  }
}
