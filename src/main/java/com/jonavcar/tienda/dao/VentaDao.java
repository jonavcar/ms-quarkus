package com.jonavcar.tienda.dao;

import com.jonavcar.tienda.context.EndpointHeaderContext;
import com.jonavcar.tienda.model.Venta;
import com.jonavcar.tienda.proxy.VentaProxy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class VentaDao {

  private static final Logger LOG = Logger.getLogger(VentaDao.class);

  @Inject
  @RestClient
  VentaProxy ventaProxy;

  @Inject
  EndpointHeaderContext endpointHeaderContext;

  public List<Venta> listar() {
    endpointHeaderContext.addHeader("X-Operation", "LIST_VENTAS");
    endpointHeaderContext.addHeader("X-Resource-Type", "venta");
    LOG.debugf("Listing ventas with specific headers");
    return ventaProxy.listar();
  }

  public Venta create(Venta venta) {
    endpointHeaderContext.addHeader("X-Operation", "CREATE_VENTA");
    endpointHeaderContext.addHeader("X-Resource-Type", "venta");
    endpointHeaderContext.addHeader("X-Business-Unit", "sales");
    endpointHeaderContext.addHeader("X-Timestamp", String.valueOf(System.currentTimeMillis()));
    LOG.debugf("Creating venta with specific headers");
    return ventaProxy.create(venta);
  }
}

