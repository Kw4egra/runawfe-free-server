/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package ru.runa.wfe.var.impl;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import ru.runa.wfe.var.Variable;
import ru.runa.wfe.var.format.VariableFormat;

@Entity
@DiscriminatorValue(value = "D")
public class DateVariable extends Variable<Date> {
    private Date object;

    @Column(name = "DATEVALUE")
    @Override
    public Date getStorableValue() {
        return object;
    }

    @Override
    public void setStorableValue(Date object) {
        this.object = object;
    }

    @Override
    public boolean supports(Object value) {
        if (super.supports(value)) {
            return true;
        }
        return value instanceof Date;
    }

    @Override
    public String toString(Object value, VariableFormat format) {
        return format.format(value);
    }
}
