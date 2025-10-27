package com.jonavcar.tienda.service;

import com.jonavcar.tienda.dao.VentaDao;
import com.jonavcar.tienda.model.Venta;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class VentaService {

    private static final Logger LOG = Logger.getLogger(VentaService.class);

    @Inject
    VentaDao ventaDao;

    public List<Venta> listar() {
        LOG.debug("Listing all ventas");
        return ventaDao.listar();
    }

    public Venta create(Venta venta) {
        LOG.debug("Creating new venta");
        return ventaDao.create(venta);
    }
}

