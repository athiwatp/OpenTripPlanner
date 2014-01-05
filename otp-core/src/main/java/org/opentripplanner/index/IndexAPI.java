/* This program is free software: you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public License
as published by the Free Software Foundation, either version 3 of
the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.index;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import lombok.Setter;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.index.model.PatternDetail;
import org.opentripplanner.index.model.PatternShort;
import org.opentripplanner.index.model.RouteShort;
import org.opentripplanner.index.model.StopShort;
import org.opentripplanner.index.model.TripShort;
import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Sets;
import com.sun.jersey.api.core.InjectParam;
import com.sun.jersey.api.spring.Autowire;

/* NOTE that in a servlet, the full path is /ws/transit (see web.xml) */

@Path("/index")
@Autowire
public class IndexAPI {

   private static final Logger LOG = LoggerFactory.getLogger(IndexAPI.class);

   private static final String MSG_404 = "FOUR ZERO FOUR"; //IMG
   
   @Setter @InjectParam 
   private GraphService graphService;

   /** Return a list of all agencies in the graph. */
   @GET
   @Path("/agencies")
   @Produces({ MediaType.APPLICATION_JSON })
   public Response getAgencies () {
       Graph graph = graphService.getGraph();
       return Response.status(Status.OK).entity(graph.getAgencies()).build();
   }

   /** Return specific agency in the graph, by ID. */
   @GET
   @Path("/agencies/{id}")
   @Produces({ MediaType.APPLICATION_JSON })
   public Response getAgency (@PathParam("id") String id) {
       Graph graph = graphService.getGraph();
       for (Agency agency : graph.getAgencies()) {
           if (agency.getId().equals(id)) {
               return Response.status(Status.OK).entity(agency).build();
           }
       }
       return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
   }
   
   /** Return a list of all stops in the graph. */
   @GET
   @Path("/stops")
   @Produces({ MediaType.APPLICATION_JSON })
   public Response getStops () {
       Graph graph = graphService.getGraph();
       Collection<Stop> stops = graph.getIndex().stopForId.values();
       return Response.status(Status.OK).entity(StopShort.list(stops)).build();
   }

   /** Return specific transit stop in the graph, by ID. */
   @GET
   @Path("/stops/{id}")
   @Produces({ MediaType.APPLICATION_JSON })
   public Response getStop (@PathParam("id") String string) {
       Graph graph = graphService.getGraph();
       AgencyAndId id = AgencyAndId.convertFromString(string);
       Stop stop = graph.getIndex().stopForId.get(id);
       if (stop != null) {
           return Response.status(Status.OK).entity(stop).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   @GET
   @Path("/stops/{id}/routes")
   @Produces({ MediaType.APPLICATION_JSON })
   public Response getRoutesForStop (@PathParam("id") String string) {
       Graph graph = graphService.getGraph();
       AgencyAndId id = AgencyAndId.convertFromString(string);
       Stop stop = graph.getIndex().stopForId.get(id);
       if (stop != null) {
           Set<Route> routes = Sets.newHashSet();
           for (TableTripPattern pattern : graph.getIndex().patternsForStop.get(stop)) {
               routes.add(pattern.route);
           }
           return Response.status(Status.OK).entity(RouteShort.list(routes)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   @GET
   @Path("/stops/{id}/patterns")
   @Produces({ MediaType.APPLICATION_JSON })
   public Response getPatternsForStop (@PathParam("id") String string) {
       Graph graph = graphService.getGraph();
       AgencyAndId id = AgencyAndId.convertFromString(string);
       Stop stop = graph.getIndex().stopForId.get(id);
       if (stop != null) {
           Collection<TableTripPattern> patterns = graph.getIndex().patternsForStop.get(stop);
           return Response.status(Status.OK).entity(PatternShort.list(patterns)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   /** Return a list of all routes in the graph. */
   @GET
   @Path("/routes")
   @Produces({ MediaType.APPLICATION_JSON })
   public Response getRoutes () {
       Graph graph = graphService.getGraph();
       Collection<Route> routes = graph.getIndex().routeForId.values();
       return Response.status(Status.OK).entity(RouteShort.list(routes)).build();
   }

   /** Return specific route in the graph, for the given ID. */
   @GET
   @Path("/routes/{id}")
   @Produces({ MediaType.APPLICATION_JSON })
   public Response getRoute (@PathParam("id") String routeIdString) {
       Graph graph = graphService.getGraph();
       AgencyAndId routeId = AgencyAndId.convertFromString(routeIdString);
       Route route = graph.getIndex().routeForId.get(routeId);
       if (route != null) {
           return Response.status(Status.OK).entity(route).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   /** Return all stop patterns used by trips on the given route. */
   @GET
   @Path("/routes/{id}/patterns")
   @Produces({ MediaType.APPLICATION_JSON })
   public Response getPatternsForRoute (@PathParam("id") String routeIdString) {
       Graph graph = graphService.getGraph();
       AgencyAndId routeId = AgencyAndId.convertFromString(routeIdString);
       Route route = graph.getIndex().routeForId.get(routeId);
       if (route != null) {
           List<TableTripPattern> patterns = graph.getIndex().patternsForRoute.get(route);
           return Response.status(Status.OK).entity(PatternShort.list(patterns)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   /** Return all stops in any pattern on a given route. */
   @GET
   @Path("/routes/{id}/stops")
   @Produces({ MediaType.APPLICATION_JSON })
   public Response getStopsForRoute (@PathParam("id") String routeIdString) {
       Graph graph = graphService.getGraph();
       AgencyAndId routeId = AgencyAndId.convertFromString(routeIdString);
       Route route = graph.getIndex().routeForId.get(routeId);
       if (route != null) {
           Set<Stop> stops = Sets.newHashSet();
           Collection<TableTripPattern> patterns = graph.getIndex().patternsForRoute.get(route);
           for (TableTripPattern pattern : patterns) {
               stops.addAll(pattern.getStops());
           }
           return Response.status(Status.OK).entity(StopShort.list(stops)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   /** Return all trips in any pattern on the given route. */
   @GET
   @Path("/routes/{id}/trips")
   @Produces({ MediaType.APPLICATION_JSON })
   public Response getTripsForRoute (@PathParam("id") String routeIdString) {
       Graph graph = graphService.getGraph();
       AgencyAndId routeId = AgencyAndId.convertFromString(routeIdString);
       Route route = graph.getIndex().routeForId.get(routeId);
       if (route != null) {
           List<Trip> trips = Lists.newArrayList();
           Collection<TableTripPattern> patterns = graph.getIndex().patternsForRoute.get(route);
           for (TableTripPattern pattern : patterns) {
               trips.addAll(pattern.getTrips());
           }
           return Response.status(Status.OK).entity(TripShort.list(trips)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }
   
   
// Disabled, results are too voluminous.
//   @Path("/trips")

   @GET
   @Path("/trips/{id}")
   @Produces({ MediaType.APPLICATION_JSON })
   public Response getTrip (@PathParam("id") String tripIdString) {
       Graph graph = graphService.getGraph();
       AgencyAndId tripId = AgencyAndId.convertFromString(tripIdString);
       Trip trip = graph.getIndex().tripForId.get(tripId);
       if (trip != null) {
           return Response.status(Status.OK).entity(trip).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   @GET
   @Path("/trips/{id}/stops")
   @Produces({ MediaType.APPLICATION_JSON })
   public Response getStopsForTrip (@PathParam("id") String tripIdString) {
       Graph graph = graphService.getGraph();
       AgencyAndId tripId = AgencyAndId.convertFromString(tripIdString);
       Trip trip = graph.getIndex().tripForId.get(tripId);
       if (trip != null) {
           TableTripPattern pattern = graph.getIndex().patternForTrip.get(trip);
           Collection<Stop> stops = pattern.getStops();
           return Response.status(Status.OK).entity(StopShort.list(stops)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   @GET
   @Path("/trips/{id}/stoptimes")
   @Produces({ MediaType.APPLICATION_JSON })
   public Response getStoptimesForTrip (@PathParam("id") String tripIdString) {
       Graph graph = graphService.getGraph();
       AgencyAndId tripId = AgencyAndId.convertFromString(tripIdString);
       Trip trip = graph.getIndex().tripForId.get(tripId);
       if (trip != null) {
           TableTripPattern pattern = graph.getIndex().patternForTrip.get(trip);
           Timetable table = pattern.getScheduledTimetable();
           return Response.status(Status.OK).entity(TripTimeShort.fromTripTimes(table, trip)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   @GET
   @Path("/patterns")
   @Produces({ MediaType.APPLICATION_JSON })
   public Response getPatterns () {
       Graph graph = graphService.getGraph();
       Collection<TableTripPattern> patterns = graph.getIndex().patternForId.values();
       return Response.status(Status.OK).entity(PatternShort.list(patterns)).build();
   }

   @GET
   @Path("/patterns/{id}")
   @Produces({ MediaType.APPLICATION_JSON })
   public Response getPattern (@PathParam("id") String string) {
       Graph graph = graphService.getGraph();
       TableTripPattern pattern = graph.getIndex().patternForId.get(string);
       if (pattern != null) {
           return Response.status(Status.OK).entity(new PatternDetail(pattern)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   @GET
   @Path("/patterns/{id}/trips")
   @Produces({ MediaType.APPLICATION_JSON })
   public Response getTripsForPattern (@PathParam("id") String string) {
       Graph graph = graphService.getGraph();
       TableTripPattern pattern = graph.getIndex().patternForId.get(string);
       if (pattern != null) {
           List<Trip> trips = pattern.getTrips();
           return Response.status(Status.OK).entity(TripShort.list(trips)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   @GET
   @Path("/patterns/{id}/stops")
   @Produces({ MediaType.APPLICATION_JSON })
   public Response getStopsForPattern (@PathParam("id") String string) {
       Graph graph = graphService.getGraph();
       TableTripPattern pattern = graph.getIndex().patternForId.get(string);
       if (pattern != null) {
           List<Stop> stops = pattern.getStops();
           return Response.status(Status.OK).entity(StopShort.list(stops)).build();
       } else { 
           return Response.status(Status.NOT_FOUND).entity(MSG_404).build();
       }
   }

   private AgencyAndId makeAgencyAndId (String string) {
       final String defaultAgency = "fjdsakl";
       String agency, id;
       int i = string.indexOf('_');
       if (i == -1) {
           agency = defaultAgency;
           id = string;
       }
       else {
           agency = string.substring(0, i);
           id = string.substring(i + 1);
       }
       return new AgencyAndId(agency, id);
   }

}