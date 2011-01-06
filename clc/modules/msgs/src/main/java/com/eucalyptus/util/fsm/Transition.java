package com.eucalyptus.util.fsm;

import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.LogLevels;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.async.Callback.Completion;
import com.eucalyptus.util.fsm.TransitionListener.Phases;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

public class Transition<P extends HasName<P>, S extends Enum<S>, T extends Enum<T>> extends TransitionAction<P> {
  private static Logger                                       LOG       = Logger.getLogger( Transition.class );
  private final AtomicInteger                                 index     = new AtomicInteger( 0 );
  private final TransitionRule<S, T>                          rule;
  private final ConcurrentMap<Integer, TransitionListener<P>> listeners = Maps.newConcurrentHashMap( );
  private final TransitionAction<P>                           action;
  
  public Transition( final TransitionRule<S, T> transitionRule, final TransitionAction<P> action, TransitionListener<P>... listeners ) {
    this.rule = transitionRule;
    this.action = action;
    for ( TransitionListener<P> listener : listeners ) {
      this.addListener( listener );
    }
  }
  
  /**
   * Add a transition listener. The stages of the transition will execute for
   * each listener in the order it was added:
   * <ol>
   * <li>{@link TransitionListener#before()}</li>
   * <li>{@link TransitionListener#leave()}</li>
   * <li>{@link TransitionListener#enter()}</li>
   * <li>{@link TransitionListener#after()}</li>
   * </ol>
   * 
   * @param transitionListener
   */
  public Transition<P, S, T> addListener( TransitionListener<P> listener ) {
    if ( LogLevels.DEBUG ) EventRecord.here( Transition.class, EventType.TRANSITION, this.toString( ), "addListener", listener.getClass( ).getSimpleName( ) );
    this.listeners.put( index.incrementAndGet( ), listener );
    return this;
  }
  
  private void removeListener( Integer key ) {
    TransitionListener<P> listener = this.listeners.remove( key );
    if ( LogLevels.DEBUG ) EventRecord.here( Transition.class, EventType.TRANSITION, this.toString( ), "removeListener", listener.getClass( ).getSimpleName( ) );
  }
  
  /**
   * @see com.eucalyptus.util.fsm.TransitionRule#getName()
   */
  public T getName( ) {
    return this.rule.getName( );
  }
  
  /**
   * @see com.eucalyptus.util.fsm.TransitionRule#getFromStateMark()
   */
  public Boolean getFromStateMark( ) {
    return this.rule.getFromStateMark( );
  }
  
  /**
   * @see com.eucalyptus.util.fsm.TransitionRule#getFromState()
   */
  public S getFromState( ) {
    return this.rule.getFromState( );
  }
  
  /**
   * @see com.eucalyptus.util.fsm.TransitionRule#getToStateMark()
   */
  public Boolean getToStateMark( ) {
    return this.rule.getToStateMark( );
  }
  
  /**
   * @see com.eucalyptus.util.fsm.TransitionRule#getToState()
   */
  public S getToState( ) {
    return this.rule.getToState( );
  }
  
  /**
   * @return
   * @see com.eucalyptus.util.fsm.TransitionRule#getErrorState()
   */
  public S getErrorState( ) {
    return this.rule.getErrorState( );
  }
  
  /**
   * @return
   * @see com.eucalyptus.util.fsm.TransitionRule#getErrorStateMark()
   */
  public Boolean getErrorStateMark( ) {
    return this.rule.getErrorStateMark( );
  }
  
  private boolean fireListeners( final TransitionListener.Phases phase, final Predicate<TransitionListener<P>> pred, P parent ) {
    for ( Entry<Integer, TransitionListener<P>> entry : this.listeners.entrySet( ) ) {
      final TransitionListener<P> tl = entry.getValue( );
      if ( LogLevels.TRACE ) {
        EventRecord.here( Transition.class, EventType.TRANSITION_LISTENER, "" + parent.getName( ), this.toString( ), phase.toString( ),//
                          entry.getKey( ).toString( ), tl.getClass( ).getName( ).replaceAll( "^(\\w.)*", "" ) ).trace( );
      }
      try {
        if ( !pred.apply( entry.getValue( ) ) ) {
          throw new TransitionListenerException( entry.getValue( ).getClass( ).getSimpleName( ) + "." + phase + "( ) returned false." );
        }
      } catch ( Throwable t ) {
        LOG.error( t, t );
        return false;
      }
    }
    return true;
  }
  
  /**
   * @see com.eucalyptus.util.fsm.TransitionListener#before()
   */
  public boolean before( final P parent ) {
    if ( this.action == null ) {
      throw new IllegalStateException( "Attempt to apply delegated transition before it is defined." );
    } else {
      this.fireListeners( Phases.before, new Predicate<TransitionListener<P>>( ) {
        @Override
        public boolean apply( TransitionListener<P> listener ) {
          return listener.before( parent );
        }
      }, parent );
      try {
        return this.action.before( parent );
      } catch ( Throwable ex ) {
        LOG.error( ex, ex );
        return false;
      }
    }
  }
  
  /**
   * @see com.eucalyptus.util.fsm.TransitionListener#leave()
   */
  @Override
  public void leave( final P parent, final Completion transitionCallback ) {
    if ( this.action == null ) {
      throw new IllegalStateException( "Attempt to apply delegated transition before it is defined." );
    } else {
      this.fireListeners( Phases.leave, new Predicate<TransitionListener<P>>( ) {
        @Override
        public boolean apply( TransitionListener<P> listener ) {
          listener.leave( parent );
          return true;
        }
      }, parent );
      this.action.leave( parent, transitionCallback );
    }
  }
  
  /**
   * @see com.eucalyptus.util.fsm.TransitionListener#enter()
   */
  public void enter( final P parent ) {
    if ( this.action == null ) {
      throw new IllegalStateException( "Attempt to apply delegated transition before it is defined." );
    } else {
      this.fireListeners( Phases.enter, new Predicate<TransitionListener<P>>( ) {
        @Override
        public boolean apply( TransitionListener<P> listener ) {
          listener.enter( parent );
          return true;
        }
      }, parent );
      this.action.enter( parent );
    }
  }
  
  /**
   * @see com.eucalyptus.util.fsm.TransitionListener#after()
   */
  public void after( final P parent ) {
    if ( this.action == null ) {
      throw new IllegalStateException( "Attempt to apply delegated transition before it is defined." );
    } else {
      this.fireListeners( Phases.after, new Predicate<TransitionListener<P>>( ) {
        @Override
        public boolean apply( TransitionListener<P> listener ) {
          listener.after( parent );
          return true;
        }
      }, parent );
      this.action.after( parent );
    }
  }
  
  @Override
  public String toString( ) {
    String actionName = "null";
    if( this.action != null ) {
      actionName = this.action.getClass( ).getName( ).replaceAll( "^(\\w.)*", "" );
    }
    Iterable<String> listenerNames = Iterables.transform( this.listeners.values( ), new Function<TransitionListener<P>, String>( ) {
      public String apply( TransitionListener<P> arg0 ) {
        return arg0.getClass( ).getName( ).replaceAll( "^(\\w.)*", "" );
      }
    } );
    return String.format( "Transition:name=%s:from=%s/%s:to=%s/%s:action=%s:listeners=%s", this.getName( ), this.getFromState( ), this.getFromStateMark( ),
                          this.getToState( ), this.getToStateMark( ), "" + this.action, listenerNames );
  }
  
  /**
   * @return the rule
   */
  public TransitionRule<S, T> getRule( ) {
    return this.rule;
  }
  
  /**
   * @see java.lang.Object#hashCode()
   * @return
   */
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.rule == null )
      ? 0
      : this.rule.hashCode( ) );
    return result;
  }
  
  /**
   * @see java.lang.Object#equals(java.lang.Object)
   * @param obj
   * @return
   */
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( getClass( ) != obj.getClass( ) ) return false;
    Transition other = ( Transition ) obj;
    return this.rule.equals( other.rule );
  }

  /**
   * @return the action
   */
  public TransitionAction<P> getAction( ) {
    return this.action;
  }
  
}