/*
 * Copyright (c) 2012, Fredrik Yttergren
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name LucidBot nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Fredrik Yttergren BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package web.models;

import api.common.HasNumericId;
import com.sun.jersey.server.linking.Ref;
import database.models.Order;
import org.hibernate.validator.constraints.NotEmpty;
import tools.validation.ValidBindings;
import web.tools.ISODateTimeAdapter;
import web.validation.Add;
import web.validation.Update;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.net.URI;
import java.util.Date;

@XmlRootElement(name = "Order")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_Order implements HasNumericId {
    /**
     * The id for this order. The id is set by the database, so clients will only use it in the URL's.
     * <p/>
     * The server will simply ignore this value if you send it in with some request.
     */
    @XmlElement(name = "ID")
    private Long id;

    /**
     * A convenience link to this entity. Only used for navigation.
     * <p/>
     * The server will simply ignore this value if you send it in with some request.
     */
    @Ref("orders/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The actual order content.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request.
     */
    @NotEmpty(message = "The order content may not be null or empty", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "Order")
    private String order;

    /**
     * The date and time at which the order was first added.
     * <p/>
     * This value is not updatable and is set automatically on add operations, so it never needs to be specified.
     */
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "Added")
    private Date added;

    /**
     * The nick of the user that first added the order.
     * <p/>
     * This value is not updatable and is set automatically on add operations, so it never needs to be specified.
     */
    @XmlElement(name = "AddedBy")
    private String addedBy;

    /**
     * The category this order belongs to, if any. Only the id needs to be populated on the category object if it's specified.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request.
     */
    @XmlElement(name = "OrderCategory")
    private RS_OrderCategory category;

    /**
     * Bindings for the order, allowing you to specify which users (as an example) the event is for.
     * <p/>
     * Not updatable, so it will be ignored for update operations. Not mandatory for add operations either.
     */
    @ValidBindings(message = "Invalid bindings", nillable = true, groups = {Add.class})
    @XmlElement(name = "Bindings")
    private RS_Bindings bindings;

    public RS_Order() {
    }

    public RS_Order(final Long id,
                    final String order,
                    final Date added,
                    final String addedBy,
                    final RS_OrderCategory category,
                    final RS_Bindings bindings) {
        this.id = id;
        this.order = order;
        this.added = added;
        this.addedBy = addedBy;
        this.category = category;
        this.bindings = bindings;
    }

    public static RS_Order fromOrder(final Order order) {
        return new RS_Order(order.getId(), order.getOrder(), order.getAdded(), order.getAddedBy(),
                RS_OrderCategory.fromOrderCategory(order.getCategory()), RS_Bindings.fromBindings(order.getBindings()));
    }

    public Long getId() {
        return id;
    }

    public String getOrder() {
        return order;
    }

    public Date getAdded() {
        return added;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public RS_OrderCategory getCategory() {
        return category;
    }

    public RS_Bindings getBindings() {
        return bindings;
    }
}
