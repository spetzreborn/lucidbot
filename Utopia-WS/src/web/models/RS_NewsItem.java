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
import database.models.NewsItem;
import web.tools.ISODateTimeAdapter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.net.URI;
import java.util.Date;

@XmlRootElement(name = "NewsItem")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_NewsItem implements HasNumericId {
    @XmlElement(name = "ID")
    private Long id;

    @Ref("news/{id}")
    @XmlElement(name = "Link")
    private URI link;

    @XmlElement(name = "Source")
    private String source;

    @XmlElement(name = "Target")
    private String target;

    @XmlElement(name = "NewsType")
    private String newsType;

    @XmlElement(name = "ItemValue")
    private String itemValue;

    @XmlElement(name = "UtoDate")
    private String utoDate;

    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "RealDate")
    private Date realDate;

    @XmlElement(name = "OriginalMessage")
    private String originalMessage;

    public RS_NewsItem() {
    }

    private RS_NewsItem(final Long id, final String source, final String target, final String newsType, final String itemValue,
                        final String utoDate, final Date realDate, final String originalMessage) {
        this.id = id;
        this.source = source;
        this.target = target;
        this.newsType = newsType;
        this.itemValue = itemValue;
        this.utoDate = utoDate;
        this.realDate = realDate;
        this.originalMessage = originalMessage;
    }

    public static RS_NewsItem fromNewsItem(final NewsItem item) {
        return new RS_NewsItem(item.getId(), item.getSource(), item.getTarget(), item.getNewsType(), item.getItemValue(), item.getUtoDate(),
                item.getRealDate(), item.getOriginalMessage());
    }

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public String getNewsType() {
        return newsType;
    }

    public String getItemValue() {
        return itemValue;
    }

    public String getUtoDate() {
        return utoDate;
    }

    public Date getRealDate() {
        return realDate;
    }

    public String getOriginalMessage() {
        return originalMessage;
    }
}
